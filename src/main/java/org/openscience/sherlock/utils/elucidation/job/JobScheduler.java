package org.openscience.sherlock.utils.elucidation.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class JobScheduler implements AutoCloseable {
    private final ExecutorService workerPool;
    private final ExecutorService dispatcher;
    private final BlockingQueue<String> queue;
    private final Map<String, JobControl> jobs;
    private final Map<String, JobSnapshot> terminalJobs;
    private final AtomicBoolean acceptingJobs;

    public JobScheduler(final int poolSize, final int queueSize) {
        this.workerPool = Executors.newFixedThreadPool(poolSize);
        this.dispatcher = Executors.newSingleThreadExecutor();
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.jobs = new ConcurrentHashMap<>();
        this.terminalJobs = new ConcurrentHashMap<>();
        this.acceptingJobs = new AtomicBoolean(true);

        this.dispatcher.execute(this::dispatchLoop);
    }

    public JobHandle scheduleJob(final Job job) {
        if (!acceptingJobs.get()) {
            throw new IllegalStateException("Scheduler is shutting down and no longer accepts new jobs.");
        }

        final String jobId = resolveJobId(job);
        final JobControl control = new JobControl(job);
        jobs.put(jobId, control);

        final boolean queued = queue.offer(jobId);
        if (!queued) {
            jobs.remove(jobId);
            throw new IllegalStateException("Job queue is full. Could not schedule job: " + jobId);
        }

        return new JobHandle(jobId, this);
    }

    public boolean cancelJob(final String jobId) {
        final JobControl control = jobs.get(jobId);
        if (control == null) {
            return false;
        }

        if (control.state.compareAndSet(JobState.QUEUED, JobState.CANCELLED)) {
            queue.remove(jobId);
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
        return true;
    }

    public boolean isCancelled(final String jobId) {
        final JobControl control = jobs.get(jobId);
        if (control != null) {
            return control.state.get() == JobState.CANCELLED;
        }

        final JobSnapshot snapshot = terminalJobs.get(jobId);
        return snapshot != null && snapshot.getState() == JobState.CANCELLED;
    }

    public boolean isDone(final String jobId) {
        final JobControl control = jobs.get(jobId);
        return control == null || control.state.get() == JobState.DONE;
    }

    public JobState getJobStatus(final String jobId) {
        final JobControl control = jobs.get(jobId);
        if (control == null) {
            final JobSnapshot snapshot = terminalJobs.get(jobId);
            return snapshot == null ? null : snapshot.getState();
        }
        return resolveJobState(control);
    }

    public JobSnapshot getJobSnapshot(final String jobId) {
        final JobControl control = jobs.get(jobId);
        if (control != null) {
            return new JobSnapshot(
                    jobId,
                    resolveJobState(control),
                    control.job.getErrorMessage(),
                    control.job.getProcessId());
        }

        return terminalJobs.get(jobId);
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
        final List<JobSnapshot> snapshot = new ArrayList<>();

        for (Map.Entry<String, JobControl> entry : jobs.entrySet()) {
            final String jobId = entry.getKey();
            final JobState jobState = resolveJobState(entry.getValue());
            if (jobState == state) {
                snapshot.add(new JobSnapshot(
                        jobId,
                        jobState,
                        entry.getValue().job.getErrorMessage(),
                        entry.getValue().job.getProcessId()));
            }
        }

        for (Map.Entry<String, JobSnapshot> entry : terminalJobs.entrySet()) {
            if (entry.getValue().getState() == state) {
                snapshot.add(entry.getValue());
            }
        }

        return Collections.unmodifiableList(snapshot);
    }

    public List<JobSnapshot> getAllJobsInQueue() {
        final List<JobSnapshot> snapshot = new ArrayList<>();
        for (Map.Entry<String, JobControl> entry : jobs.entrySet()) {
            final String jobId = entry.getKey();
            final JobControl control = entry.getValue();

            snapshot.add(new JobSnapshot(
                    jobId,
                    resolveJobState(control),
                    control.job.getErrorMessage(),
                    control.job.getProcessId()));
        }

        return Collections.unmodifiableList(snapshot);
    }

    public List<JobSnapshot> getAllTerminalJobs() {
        final List<JobSnapshot> snapshot = new ArrayList<>();
        for (Map.Entry<String, JobSnapshot> entry : terminalJobs.entrySet()) {
            snapshot.add(entry.getValue());
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
        jobs.clear();
        terminalJobs.clear();
    }

    @Override
    public void close() {
        shutdown();
    }

    private void dispatchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final String jobId = queue.take();
                final JobControl control = jobs.get(jobId);
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

            if (control.state.get() != JobState.CANCELLED) {
                control.job.run();
            }
        } catch (Throwable throwable) {
            control.job.setErrorMessage(
                    throwable.getMessage() == null || throwable.getMessage().isBlank()
                            ? throwable.getClass().getSimpleName()
                            : throwable.getMessage());
            control.state.set(JobState.ERROR);
        } finally {
            control.state.compareAndSet(JobState.RUNNING, JobState.DONE);
            archiveAndRemove(jobId, control);
        }
    }

    private void archiveAndRemove(final String jobId, final JobControl control) {
        final JobState finalState = resolveJobState(control);
        if (finalState == JobState.CANCELLED || finalState == JobState.DONE || finalState == JobState.ERROR) {
            terminalJobs.put(jobId, new JobSnapshot(
                    jobId,
                    finalState,
                    control.job.getErrorMessage(),
                    control.job.getProcessId()));
        }
        control.job.clearProcess();
        jobs.remove(jobId);
    }

    private static String resolveJobId(final Job job) {
        if (job.getId() == null || job.getId().isBlank()) {
            final String generatedId = UUID.randomUUID().toString();
            job.setId(generatedId);
            return generatedId;
        }
        return job.getId();
    }

    private static JobState resolveJobState(final JobControl control) {
        return control.state.get();
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

    public static final class JobSnapshot {
        private final String jobId;
        private final JobState state;
        private final String errorMessage;
        private final Long processId;

        private JobSnapshot(final String jobId, final JobState state, final String errorMessage, final Long processId) {
            this.jobId = jobId;
            this.state = state;
            this.errorMessage = errorMessage;
            this.processId = processId;
        }

        public String getJobId() {
            return jobId;
        }

        public JobState getState() {
            return state;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Long getProcessId() {
            return processId;
        }
    }

    public enum JobState {
        QUEUED,
        RUNNING,
        CANCELLED,
        DONE,
        ERROR
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