package org.openscience.sherlock.utils.elucidation.job;

import java.lang.reflect.Proxy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.openscience.sherlock.dbservice.job.model.JobRecord;
import org.openscience.sherlock.dbservice.job.repository.JobRecordRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSchedulerTest {

    @Test
    void queuedCancellationIsPersistedAsCancelled() throws Exception {
        final CountDownLatch firstJobStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirstJob = new CountDownLatch(1);
        final InMemoryJobRecordRepository repository = new InMemoryJobRecordRepository();

        try (JobScheduler scheduler = new JobScheduler(1, 10, repository.createProxy())) {
            final Job firstJob = new Job("job-persist-first", "persist-first", null, "request-first") {
                @Override
                public void run() {
                    firstJobStarted.countDown();
                    try {
                        releaseFirstJob.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            };

            final Job secondJob = new Job("job-persist-cancelled", "persist-cancelled", null, "request-cancelled") {
                @Override
                public void run() {
                }
            };

            scheduler.scheduleJob(firstJob);
            assertTrue(firstJobStarted.await(1, TimeUnit.SECONDS), "First job should start");

            final JobScheduler.JobHandle secondHandle = scheduler.scheduleJob(secondJob);
            assertTrue(secondHandle.cancel(), "Queued job should be cancellable");

            releaseFirstJob.countDown();
            waitUntil(secondHandle::isDone, Duration.ofSeconds(2));

            final JobRecord cancelledRecord = repository.get("job-persist-cancelled");
            assertNotNull(cancelledRecord, "Cancelled job should be persisted");
            assertEquals(JobState.CANCELLED, cancelledRecord.getState());
            assertEquals("request-cancelled", cancelledRecord.getRequestData());
        }
    }

    @Test
    void runningCancellationIsPersistedAsCancelled() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch finished = new CountDownLatch(1);
        final InMemoryJobRecordRepository repository = new InMemoryJobRecordRepository();

        try (JobScheduler scheduler = new JobScheduler(1, 10, repository.createProxy())) {
            final Job longRunningJob = new Job("job-persist-running-cancel", "persist-running-cancel", null,
                    "request-running-cancel") {
                @Override
                public void run() {
                    started.countDown();
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            Thread.sleep(25);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(longRunningJob);
            assertTrue(started.await(1, TimeUnit.SECONDS), "Job should start execution");

            assertTrue(handle.cancel(), "Running job should be cancellable");
            assertTrue(finished.await(2, TimeUnit.SECONDS), "Cancelled running job should finish quickly");
            waitUntil(handle::isDone, Duration.ofSeconds(2));

            final JobRecord cancelledRecord = repository.get("job-persist-running-cancel");
            assertNotNull(cancelledRecord, "Cancelled running job should be persisted");
            assertEquals(JobState.CANCELLED, cancelledRecord.getState());
            assertEquals("request-running-cancel", cancelledRecord.getRequestData());
        }
    }

    @Test
    void cancellationExceptionIsPersistedAsCancelled() throws Exception {
        final InMemoryJobRecordRepository repository = new InMemoryJobRecordRepository();

        try (JobScheduler scheduler = new JobScheduler(1, 10, repository.createProxy())) {
            final Job cancelledJob = new Job("job-persist-timeout-cancel", "persist-timeout-cancel", null,
                    "request-timeout-cancel") {
                @Override
                public void run() {
                    throw new JobCancelledException("time limit reached");
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(cancelledJob);
            waitUntil(handle::isDone, Duration.ofSeconds(2));

            final JobRecord cancelledRecord = repository.get("job-persist-timeout-cancel");
            assertNotNull(cancelledRecord, "Time-limit cancellation should be persisted");
            assertEquals(JobState.CANCELLED, cancelledRecord.getState());
            assertEquals("time limit reached", cancelledRecord.getErrorMessage());
            assertEquals("request-timeout-cancel", cancelledRecord.getRequestData());
        }
    }

    @Test
    void genuineFailureIsPersistedAsError() throws Exception {
        final InMemoryJobRecordRepository repository = new InMemoryJobRecordRepository();

        try (JobScheduler scheduler = new JobScheduler(1, 10, repository.createProxy())) {
            final Job failingJob = new Job("job-persist-error", "persist-error", null, "request-error") {
                @Override
                public void run() {
                    throw new IllegalStateException("boom");
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(failingJob);
            waitUntil(handle::isDone, Duration.ofSeconds(2));

            final JobRecord erroredRecord = repository.get("job-persist-error");
            assertNotNull(erroredRecord, "Failed job should be persisted");
            assertEquals(JobState.ERROR, erroredRecord.getState());
            assertEquals("boom", erroredRecord.getErrorMessage());
            assertEquals("request-error", erroredRecord.getRequestData());
        }
    }

    @Test
    void getJobStatusReturnsSpecificJobState() throws Exception {
        final CountDownLatch firstJobStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirstJob = new CountDownLatch(1);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job firstJob = new Job("job-status-1", "status-1") {
                @Override
                public void run() {
                    firstJobStarted.countDown();
                    try {
                        releaseFirstJob.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            };

            final Job secondJob = new Job("job-status-2", "status-2") {
                @Override
                public void run() {
                }
            };

            scheduler.scheduleJob(firstJob);
            assertTrue(firstJobStarted.await(1, TimeUnit.SECONDS), "First job should start");
            final JobScheduler.JobHandle secondHandle = scheduler.scheduleJob(secondJob);

            assertEquals(JobState.RUNNING, scheduler.getJobStatus("job-status-1"));
            assertTrue(
                    Set.of(JobState.QUEUED, JobState.RUNNING)
                            .contains(scheduler.getJobStatus("job-status-2")),
                    "Second job should be queued or already running");
            assertEquals(JobState.UNKNOWN, scheduler.getJobStatus("job-does-not-exist"),
                    "Unknown job should return UNKNOWN status");

            releaseFirstJob.countDown();
            waitUntil(secondHandle::isDone, Duration.ofSeconds(2));
        }
    }

    @Test
    void getAllJobsReturnsCurrentSnapshot() throws Exception {
        final CountDownLatch firstJobStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirstJob = new CountDownLatch(1);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job firstJob = new Job("job-snapshot-1", "snapshot-1") {
                @Override
                public void run() {
                    firstJobStarted.countDown();
                    try {
                        releaseFirstJob.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            };

            final Job secondJob = new Job("job-snapshot-2", "snapshot-2") {
                @Override
                public void run() {
                }
            };

            scheduler.scheduleJob(firstJob);
            assertTrue(firstJobStarted.await(1, TimeUnit.SECONDS), "First job should start");
            final JobScheduler.JobHandle secondHandle = scheduler.scheduleJob(secondJob);

            final List<JobSnapshot> jobs = scheduler.getAllJobsInQueue();
            assertTrue(jobs.stream().anyMatch(job -> job.getJobId().equals("job-snapshot-1")),
                    "Running job should be present in snapshot");
            assertTrue(jobs.stream().anyMatch(job -> job.getJobId().equals("job-snapshot-2")),
                    "Queued job should be present in snapshot");
            assertEquals(JobState.RUNNING,
                    jobs.stream().filter(job -> job.getJobId().equals("job-snapshot-1")).findFirst().get().getState());
            assertTrue(
                    Set.of(JobState.QUEUED, JobState.RUNNING)
                            .contains(jobs.stream().filter(job -> job.getJobId().equals("job-snapshot-2")).findFirst()
                                    .get().getState()),
                    "Second job should be queued or already running");

            releaseFirstJob.countDown();
            waitUntil(secondHandle::isDone, Duration.ofSeconds(2));
            waitUntil(() -> scheduler.getAllJobsInQueue().isEmpty(), Duration.ofSeconds(2));
            assertTrue(scheduler.getAllJobsInQueue().isEmpty(), "Snapshot should be empty when all jobs are done");
        }
    }

    @Test
    void scheduledJobCompletesSuccessfully() throws Exception {
        final AtomicBoolean executed = new AtomicBoolean(false);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job job = new Job("job-complete", "complete") {
                @Override
                public void run() {
                    executed.set(true);
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(job);

            waitUntil(handle::isDone, Duration.ofSeconds(2));

            assertTrue(executed.get(), "Job should run to completion");
            assertTrue(handle.isDone(), "Handle should report completion");
        }
    }

    @Test
    void cancelledJobBeforeExecutionDoesNotRun() throws Exception {
        final CountDownLatch firstJobStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirstJob = new CountDownLatch(1);
        final AtomicInteger secondJobRuns = new AtomicInteger(0);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job firstJob = new Job("job-first", "first") {
                @Override
                public void run() {
                    firstJobStarted.countDown();
                    try {
                        releaseFirstJob.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            };

            final Job secondJob = new Job("job-second", "second") {
                @Override
                public void run() {
                    secondJobRuns.incrementAndGet();
                }
            };

            scheduler.scheduleJob(firstJob);
            assertTrue(firstJobStarted.await(1, TimeUnit.SECONDS), "First job should start");

            final JobScheduler.JobHandle secondHandle = scheduler.scheduleJob(secondJob);
            assertTrue(secondHandle.cancel(), "Second job should be cancellable");

            releaseFirstJob.countDown();
            waitUntil(secondHandle::isDone, Duration.ofSeconds(2));

            assertTrue(secondHandle.isDone(), "Cancelled job should be marked done");
            assertTrue(secondJobRuns.get() == 0, "Cancelled job should never execute");
        }
    }

    @Test
    void cancellingRunningJobInterruptsExecution() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicBoolean interrupted = new AtomicBoolean(false);
        final CountDownLatch finished = new CountDownLatch(1);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job longRunningJob = new Job("job-running", "running") {
                @Override
                public void run() {
                    started.countDown();
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            Thread.sleep(25);
                        }
                    } catch (InterruptedException e) {
                        interrupted.set(true);
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(longRunningJob);
            assertTrue(started.await(1, TimeUnit.SECONDS), "Job should start execution");

            assertTrue(handle.cancel(), "Running job should be cancellable");
            assertTrue(finished.await(2, TimeUnit.SECONDS), "Cancelled running job should finish quickly");

            waitUntil(handle::isDone, Duration.ofSeconds(2));

            assertTrue(interrupted.get(), "Running job should observe interruption when cancelled");
            assertTrue(handle.isDone(), "Cancelled running job should be marked done");
        }
    }

    @Test
    void finishedAndCancelledJobsCanBeFetchedFromHistory() throws Exception {
        final CountDownLatch firstJobStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirstJob = new CountDownLatch(1);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job firstJob = new Job("job-history-done", "history-done") {
                @Override
                public void run() {
                    firstJobStarted.countDown();
                    try {
                        releaseFirstJob.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            };

            final Job secondJob = new Job("job-history-cancelled", "history-cancelled") {
                @Override
                public void run() {
                }
            };

            final JobScheduler.JobHandle firstHandle = scheduler.scheduleJob(firstJob);
            assertTrue(firstJobStarted.await(1, TimeUnit.SECONDS), "First job should start");

            final JobScheduler.JobHandle secondHandle = scheduler.scheduleJob(secondJob);
            assertTrue(secondHandle.cancel(), "Second job should be cancellable while queued");

            releaseFirstJob.countDown();
            waitUntil(firstHandle::isDone, Duration.ofSeconds(2));

            final List<JobSnapshot> finishedJobs = scheduler.getFinishedJobs();
            final List<JobSnapshot> cancelledJobs = scheduler.getCancelledJobs();

            assertTrue(finishedJobs.stream().anyMatch(job -> job.getJobId().equals("job-history-done")),
                    "Finished history should contain done job");
            assertTrue(cancelledJobs.stream().anyMatch(job -> job.getJobId().equals("job-history-cancelled")),
                    "Cancelled history should contain cancelled job");

            assertEquals(JobState.DONE, scheduler.getJobStatus("job-history-done"));
            assertEquals(JobState.CANCELLED, scheduler.getJobStatus("job-history-cancelled"));

            assertTrue(scheduler.getJobsByState(JobState.DONE)
                    .stream()
                    .anyMatch(job -> job.getJobId().equals("job-history-done")));
            assertTrue(scheduler.getJobsByState(JobState.CANCELLED)
                    .stream()
                    .anyMatch(job -> job.getJobId().equals("job-history-cancelled")));
        }
    }

    @Test
    void failedJobIsMarkedAsErrorAndArchived() throws Exception {
        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job failingJob = new Job("job-history-error", "history-error") {
                @Override
                public void run() {
                    throw new RuntimeException("boom");
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(failingJob);
            waitUntil(handle::isDone, Duration.ofSeconds(2));

            assertEquals(JobState.ERROR, scheduler.getJobStatus("job-history-error"));
            assertTrue(scheduler.getErroredJobs().stream().anyMatch(job -> job.getJobId().equals("job-history-error")
                    && "boom".equals(job.getErrorMessage())),
                    "Errored history should contain failed job");
            assertTrue(scheduler.getJobsByState(JobState.ERROR)
                    .stream()
                    .anyMatch(job -> job.getJobId().equals("job-history-error")));
            assertEquals("boom", scheduler.getAllTerminalJobs()
                    .stream()
                    .filter(job -> job.getJobId().equals("job-history-error"))
                    .findFirst()
                    .get()
                    .getErrorMessage());
        }
    }

    @Test
    void runningJobExposesProcessIdAndCancellationStopsProcess() throws Exception {
        final CountDownLatch processStarted = new CountDownLatch(1);
        final AtomicBoolean processStopped = new AtomicBoolean(false);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job processJob = new Job("job-process", "process") {
                @Override
                public void run() {
                    try {
                        final Process process = new ProcessBuilder("sh", "-c", "sleep 30").start();
                        attachProcess(process);
                        processStarted.countDown();
                        process.waitFor();
                        processStopped.set(!process.isAlive());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(processJob);
            assertTrue(processStarted.await(2, TimeUnit.SECONDS), "Process-backed job should start");

            waitUntil(() -> scheduler.getAllJobsInQueue().stream()
                    .anyMatch(job -> job.getJobId().equals("job-process") && job.getProcessId() != null),
                    Duration.ofSeconds(2));

            final JobSnapshot runningSnapshot = scheduler.getAllJobsInQueue()
                    .stream()
                    .filter(job -> job.getJobId().equals("job-process"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(runningSnapshot.getProcessId() != null && runningSnapshot.getProcessId() > 0,
                    "Running snapshot should expose process id");

            assertTrue(handle.cancel(), "Process-backed job should be cancellable");
            waitUntil(handle::isDone, Duration.ofSeconds(5));

            final JobSnapshot cancelledSnapshot = scheduler.getCancelledJobs()
                    .stream()
                    .filter(job -> job.getJobId().equals("job-process"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(runningSnapshot.getProcessId(), cancelledSnapshot.getProcessId());
            assertTrue(processStopped.get(), "Process should no longer be alive after cancellation");
        }
    }

    @Test
    void cancellingRunningJobKillsChildProcessTree() throws Exception {
        final CountDownLatch processTreeStarted = new CountDownLatch(1);
        final AtomicLong childPid = new AtomicLong(-1L);
        final AtomicBoolean processStopped = new AtomicBoolean(false);

        try (JobScheduler scheduler = new JobScheduler(1, 10)) {
            final Job processTreeJob = new Job("job-process-tree", "process-tree") {
                @Override
                public void run() {
                    try {
                        final Process process = new ProcessBuilder("sh", "-c", "sleep 30 & echo $!; wait").start();
                        attachProcess(process);

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()))) {
                            final String pidLine = reader.readLine();
                            if (pidLine != null && !pidLine.isBlank()) {
                                childPid.set(Long.parseLong(pidLine.trim()));
                            }
                        }

                        processTreeStarted.countDown();
                        process.waitFor();
                        processStopped.set(!process.isAlive());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            final JobScheduler.JobHandle handle = scheduler.scheduleJob(processTreeJob);
            assertTrue(processTreeStarted.await(2, TimeUnit.SECONDS), "Process tree job should start");
            waitUntil(() -> childPid.get() > 0, Duration.ofSeconds(2));

            assertTrue(ProcessHandle.of(childPid.get()).map(processHandle -> processHandle.isAlive()).orElse(false),
                    "Spawned child process should be alive before cancellation");

            assertTrue(handle.cancel(), "Process tree job should be cancellable");
            waitUntil(handle::isDone, Duration.ofSeconds(5));

            assertTrue(processStopped.get(), "Root process should no longer be alive after cancellation");
            waitUntil(
                    () -> !ProcessHandle.of(childPid.get()).map(processHandle -> processHandle.isAlive()).orElse(false),
                    Duration.ofSeconds(2));
            assertFalse(ProcessHandle.of(childPid.get()).map(processHandle -> processHandle.isAlive()).orElse(false),
                    "Child process should no longer be alive after cancellation");
        }
    }

    private static void waitUntil(final BooleanSupplier condition, final Duration timeout) throws InterruptedException {
        final long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "Condition was not met before timeout");
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private static final class InMemoryJobRecordRepository {
        private final Map<String, JobRecord> records = new HashMap<>();

        private JobRecordRepository createProxy() {
            return (JobRecordRepository) Proxy.newProxyInstance(
                    JobRecordRepository.class.getClassLoader(),
                    new Class<?>[] { JobRecordRepository.class },
                    (proxy, method, args) -> {
                        final String methodName = method.getName();

                        if ("findById".equals(methodName)) {
                            return Optional.ofNullable(copy(records.get((String) args[0])));
                        }
                        if ("save".equals(methodName)) {
                            final JobRecord record = copy((JobRecord) args[0]);
                            records.put(record.getJobId(), record);
                            return copy(record);
                        }
                        if ("deleteById".equals(methodName)) {
                            records.remove((String) args[0]);
                            return null;
                        }
                        if ("findAllByState".equals(methodName)) {
                            final JobState state = (JobState) args[0];
                            return records.values().stream()
                                    .filter(record -> record.getState() == state)
                                    .map(InMemoryJobRecordRepository::copy)
                                    .toList();
                        }
                        if ("findAllByStateIn".equals(methodName)) {
                            final Collection<?> rawStates = (Collection<?>) args[0];
                            return records.values().stream()
                                    .filter(record -> rawStates.contains(record.getState()))
                                    .map(InMemoryJobRecordRepository::copy)
                                    .toList();
                        }
                        if ("findAll".equals(methodName)) {
                            return new ArrayList<>(
                                    records.values().stream().map(InMemoryJobRecordRepository::copy).toList());
                        }
                        if ("findByJobId".equals(methodName)) {
                            return copy(records.get((String) args[0]));
                        }
                        if ("equals".equals(methodName)) {
                            return proxy == args[0];
                        }
                        if ("hashCode".equals(methodName)) {
                            return System.identityHashCode(proxy);
                        }
                        if ("toString".equals(methodName)) {
                            return "InMemoryJobRecordRepositoryProxy";
                        }

                        throw new UnsupportedOperationException("Unsupported repository method: " + methodName);
                    });
        }

        private JobRecord get(final String jobId) {
            return copy(records.get(jobId));
        }

        private static JobRecord copy(final JobRecord source) {
            if (source == null) {
                return null;
            }

            final JobRecord copy = new JobRecord();
            copy.setJobId(source.getJobId());
            copy.setName(source.getName());
            copy.setState(source.getState());
            copy.setErrorMessage(source.getErrorMessage());
            copy.setProcessId(source.getProcessId());
            copy.setRequestData(source.getRequestData());
            copy.setCreatedAt(source.getCreatedAt());
            copy.setLastModifiedAt(source.getLastModifiedAt());
            return copy;
        }
    }
}
