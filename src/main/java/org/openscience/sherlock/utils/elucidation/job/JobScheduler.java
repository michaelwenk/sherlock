package org.openscience.sherlock.utils.elucidation.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openscience.sherlock.dbservice.job.model.JobRecord;
import org.openscience.sherlock.dbservice.job.repository.JobRecordRepository;
import org.openscience.sherlock.utils.IdGenerator;

public class JobScheduler implements AutoCloseable {
    private final ExecutorService workerPool;
    private final ExecutorService dispatcher;
    private final BlockingQueue<String> queue;
    private final Map<String, JobControl> activeJobs;
    private final JobRecordRepository jobRecordRepository;
    private final AtomicBoolean acceptingJobs;

    public JobScheduler(
            final int poolSize,
            final int queueSize,
            final JobRecordRepository jobRecordRepository) {
        this.workerPool = Executors.newFixedThreadPool(poolSize);
        this.dispatcher = Executors.newSingleThreadExecutor();
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.activeJobs = new ConcurrentHashMap<>();
        this.jobRecordRepository = Objects.requireNonNull(jobRecordRepository,
                "JobRecordRepository must not be null. JobScheduler is database-backed only.");
        this.acceptingJobs = new AtomicBoolean(true);

        reconcileInFlightJobsFromPreviousRuns();
        this.dispatcher.execute(this::dispatchLoop);
    }

    public JobHandle scheduleJob(final Job job) {
        if (!acceptingJobs.get()) {
            throw new IllegalStateException("Scheduler is shutting down and no longer accepts new jobs.");
        }

        final String jobId = resolveJobId(job);
        final JobControl control = new JobControl(job);
        activeJobs.put(jobId, control);
        persistJobState(jobId, job.getName(), JobState.QUEUED, null, null, job.getRequestData(),
                job.getRequestPasswordHash());

        final boolean queued = queue.offer(jobId);
        if (!queued) {
            activeJobs.remove(jobId);
            deletePersistedJob(jobId);
            throw new IllegalStateException("Job queue is full. Could not schedule job: " + jobId);
        }

        return new JobHandle(jobId, this);
    }

    public boolean cancelJob(final String jobId) {
        final JobControl control = activeJobs.get(jobId);
        if (control == null) {
            final JobSnapshot persisted = findPersistedSnapshot(jobId);
            return persisted != null && persisted.getState() == JobState.CANCELLED;
        }

        final String errorMessage = control.job.getErrorMessage();

        if (control.state.compareAndSet(JobState.QUEUED, JobState.CANCELLED)) {
            queue.remove(jobId);
            persistJobState(jobId, control.job.getName(), JobState.CANCELLED, errorMessage, control.job.getProcessId(),
                    control.job.getRequestData(), control.job.getRequestPasswordHash());
            archiveAndRemove(jobId, control);
            return true;
        }

        if (!control.state.compareAndSet(JobState.RUNNING, JobState.CANCELLED)) {
            return false;
        }

        control.job.destroyProcess();

        final Future<?> runningFuture = control.future;
        if (runningFuture != null) {
            runningFuture.cancel(true);
        }

        control.state.set(JobState.CANCELLED);
        persistJobState(jobId, control.job.getName(), JobState.CANCELLED, errorMessage, control.job.getProcessId(),
                control.job.getRequestData(), control.job.getRequestPasswordHash());

        return true;
    }

    public boolean isCancelled(final String jobId) {
        final JobControl control = activeJobs.get(jobId);
        if (control != null) {
            return control.state.get() == JobState.CANCELLED;
        }

        final JobSnapshot snapshot = findPersistedSnapshot(jobId);
        return snapshot != null && snapshot.getState() == JobState.CANCELLED;
    }

    public boolean isDone(final String jobId) {
        final JobControl control = activeJobs.get(jobId);
        if (control != null) {
            final JobState state = control.state.get();
            return state == JobState.DONE || state == JobState.CANCELLED || state == JobState.ERROR;
        }

        final JobSnapshot snapshot = findPersistedSnapshot(jobId);
        if (snapshot == null) {
            return false;
        }

        final JobState state = snapshot.getState();
        return state == JobState.DONE || state == JobState.CANCELLED || state == JobState.ERROR;
    }

    public JobState getJobStatus(final String jobId) {
        final JobControl control = activeJobs.get(jobId);
        if (control == null) {
            final JobSnapshot snapshot = findPersistedSnapshot(jobId);
            return snapshot == null ? JobState.UNKNOWN : snapshot.getState();
        }
        return resolveJobState(control);
    }

    public JobSnapshot getJobSnapshot(final String jobId) {
        final JobControl control = activeJobs.get(jobId);
        if (control != null) {
            return new JobSnapshot(
                    jobId,
                    resolveJobState(control),
                    control.job.getErrorMessage(),
                    control.job.getProcessId());
        }

        return findPersistedSnapshot(jobId);
    }

    public List<JobSnapshot> getQueuedJobs() {
        return getJobsByState(JobState.QUEUED);
    }

    public List<JobSnapshot> getRunningJobs() {
        return getJobsByState(JobState.RUNNING);
    }

    public List<JobSnapshot> getFinishedJobs() {
        return getJobsByState(JobState.DONE);
    }

    public List<JobSnapshot> getCancelledJobs() {
        return getJobsByState(JobState.CANCELLED);
    }

    public List<JobSnapshot> getErroredJobs() {
        return getJobsByState(JobState.ERROR);
    }

    public List<JobSnapshot> getJobsByState(final JobState state) {
        final Map<String, JobSnapshot> snapshotById = new LinkedHashMap<>();

        if (state == JobState.QUEUED || state == JobState.RUNNING) {
            for (Map.Entry<String, JobControl> entry : activeJobs.entrySet()) {
                final String jobId = entry.getKey();
                final JobControl control = entry.getValue();
                final JobState jobState = resolveJobState(control);
                if (jobState == state) {
                    snapshotById.put(jobId, new JobSnapshot(
                            jobId,
                            jobState,
                            control.job.getErrorMessage(),
                            control.job.getProcessId()));
                }
            }
        }

        for (JobRecord jobRecord : jobRecordRepository.findAllByState(state)) {
            snapshotById.put(jobRecord.getJobId(), toSnapshot(jobRecord));
        }

        return Collections.unmodifiableList(new ArrayList<>(snapshotById.values()));
    }

    public List<JobSnapshot> getAllJobsInQueue() {
        final Map<String, JobSnapshot> snapshotById = new LinkedHashMap<>();

        for (Map.Entry<String, JobControl> entry : activeJobs.entrySet()) {
            final String jobId = entry.getKey();
            final JobControl control = entry.getValue();
            final JobState state = resolveJobState(control);
            if (state == JobState.QUEUED || state == JobState.RUNNING) {
                snapshotById.put(jobId, new JobSnapshot(
                        jobId,
                        state,
                        control.job.getErrorMessage(),
                        control.job.getProcessId()));
            }
        }

        for (JobRecord jobRecord : jobRecordRepository.findAllByStateIn(
                EnumSet.of(JobState.QUEUED, JobState.RUNNING))) {
            snapshotById.put(jobRecord.getJobId(), toSnapshot(jobRecord));
        }

        return Collections.unmodifiableList(new ArrayList<>(snapshotById.values()));
    }

    public List<JobSnapshot> getAllTerminalJobs() {
        final List<JobSnapshot> snapshot = new ArrayList<>();
        for (JobRecord jobRecord : jobRecordRepository.findAllByStateIn(
                EnumSet.of(JobState.CANCELLED, JobState.DONE, JobState.ERROR))) {
            snapshot.add(toSnapshot(jobRecord));
        }
        return Collections.unmodifiableList(snapshot);
    }

    public void shutdown() {
        acceptingJobs.set(false);
        dispatcher.shutdownNow();
        workerPool.shutdown();
    }

    public void shutdownNow() {
        acceptingJobs.set(false);
        dispatcher.shutdownNow();
        workerPool.shutdownNow();
        queue.clear();
        activeJobs.clear();
    }

    @Override
    public void close() {
        shutdown();
    }

    private void dispatchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final String jobId = queue.take();
                final JobControl control = activeJobs.get(jobId);
                if (control == null) {
                    continue;
                }

                final Future<?> future = workerPool.submit(() -> executeJob(jobId, control));
                control.future = future;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void executeJob(final String jobId, final JobControl control) {
        try {
            if (!control.state.compareAndSet(JobState.QUEUED, JobState.RUNNING)) {
                return;
            }

            persistJobState(jobId, control.job.getName(), JobState.RUNNING, control.job.getErrorMessage(),
                    control.job.getProcessId(), control.job.getRequestData(), control.job.getRequestPasswordHash());

            if (control.state.get() != JobState.CANCELLED) {
                control.job.run();
            }
        } catch (Throwable throwable) {
            final String message = throwable.getMessage() == null || throwable.getMessage().isBlank()
                    ? throwable.getClass().getSimpleName()
                    : throwable.getMessage();
            control.job.setErrorMessage(message);

            if (throwable instanceof JobCancelledException || control.state.get() == JobState.CANCELLED) {
                control.state.set(JobState.CANCELLED);
                persistJobState(jobId, control.job.getName(), JobState.CANCELLED, control.job.getErrorMessage(),
                        control.job.getProcessId(), control.job.getRequestData(), control.job.getRequestPasswordHash());
            } else {
                control.state.set(JobState.ERROR);
                persistJobState(jobId, control.job.getName(), JobState.ERROR, control.job.getErrorMessage(),
                        control.job.getProcessId(), control.job.getRequestData(), control.job.getRequestPasswordHash());
            }
        } finally {
            control.state.compareAndSet(JobState.RUNNING, JobState.DONE);
            archiveAndRemove(jobId, control);
        }
    }

    private void archiveAndRemove(final String jobId, final JobControl control) {
        final JobState finalState = resolveJobState(control);
        persistJobState(jobId, control.job.getName(), finalState, control.job.getErrorMessage(),
                control.job.getProcessId(), control.job.getRequestData(), control.job.getRequestPasswordHash());
        control.job.clearProcess();
        activeJobs.remove(jobId);
    }

    private static String resolveJobId(final Job job) {
        if (job.getId() == null || job.getId().isBlank()) {
            final String generatedId = IdGenerator.generateId();
            job.setId(generatedId);
            return generatedId;
        }
        return job.getId();
    }

    private static JobState resolveJobState(final JobControl control) {
        return control.state.get();
    }

    private void reconcileInFlightJobsFromPreviousRuns() {
        for (JobRecord jobRecord : jobRecordRepository.findAllByStateIn(
                EnumSet.of(JobState.QUEUED, JobState.RUNNING))) {
            jobRecord.setState(JobState.ERROR);
            jobRecord.setErrorMessage("Scheduler restarted before job completion");
            jobRecordRepository.save(jobRecord);
        }
    }

    private void persistJobState(
            final String jobId,
            final String name,
            final JobState state,
            final String errorMessage,
            final Long processId,
            final String requestData,
            final String requestPasswordHash) {
        final JobRecord record = jobRecordRepository.findById(jobId).orElseGet(JobRecord::new);
        record.setJobId(jobId);
        final String persistedName = (name == null || name.isBlank()) ? jobId : name;
        record.setName(persistedName);
        record.setState(state);
        record.setErrorMessage(errorMessage);
        record.setProcessId(processId);
        record.setRequestData(requestData == null ? "{}" : requestData);
        record.setRequestPasswordHash(requestPasswordHash);
        jobRecordRepository.save(record);
    }

    private JobSnapshot findPersistedSnapshot(final String jobId) {
        return jobRecordRepository.findById(jobId).map(JobScheduler::toSnapshot).orElse(null);
    }

    private static JobSnapshot toSnapshot(final JobRecord jobRecord) {
        return new JobSnapshot(
                jobRecord.getJobId(),
                jobRecord.getState(),
                jobRecord.getErrorMessage(),
                jobRecord.getProcessId());
    }

    private void deletePersistedJob(final String jobId) {
        jobRecordRepository.deleteById(jobId);
    }

    public static final class JobHandle {
        private final String jobId;
        private final JobScheduler scheduler;

        private JobHandle(final String jobId, final JobScheduler scheduler) {
            this.jobId = jobId;
            this.scheduler = scheduler;
        }

        public String getJobId() {
            return jobId;
        }

        public boolean cancel() {
            return scheduler.cancelJob(jobId);
        }

        public boolean isCancelled() {
            return scheduler.isCancelled(jobId);
        }

        public boolean isDone() {
            return scheduler.isDone(jobId);
        }
    }

    private static final class JobControl {
        private final Job job;
        private final AtomicReference<JobState> state;
        private volatile Future<?> future;

        private JobControl(final Job job) {
            this.job = job;
            this.state = new AtomicReference<>(JobState.QUEUED);
        }
    }
}