package org.openscience.sherlock.utils.elucidation.job;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSchedulerTest {

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

            assertEquals(JobScheduler.JobState.RUNNING, scheduler.getJobStatus("job-status-1"));
            assertTrue(
                    Set.of(JobScheduler.JobState.QUEUED, JobScheduler.JobState.RUNNING)
                            .contains(scheduler.getJobStatus("job-status-2")),
                    "Second job should be queued or already running");
            assertNull(scheduler.getJobStatus("job-does-not-exist"), "Unknown job should return null status");

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

            final List<JobScheduler.JobSnapshot> jobs = scheduler.getAllJobsInQueue();
            assertTrue(jobs.stream().anyMatch(job -> job.getJobId().equals("job-snapshot-1")),
                    "Running job should be present in snapshot");
            assertTrue(jobs.stream().anyMatch(job -> job.getJobId().equals("job-snapshot-2")),
                    "Queued job should be present in snapshot");
            assertEquals(JobScheduler.JobState.RUNNING,
                    jobs.stream().filter(job -> job.getJobId().equals("job-snapshot-1")).findFirst().get().getState());
            assertTrue(
                    Set.of(JobScheduler.JobState.QUEUED, JobScheduler.JobState.RUNNING)
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

            final List<JobScheduler.JobSnapshot> finishedJobs = scheduler.getFinishedJobs();
            final List<JobScheduler.JobSnapshot> cancelledJobs = scheduler.getCancelledJobs();

            assertTrue(finishedJobs.stream().anyMatch(job -> job.getJobId().equals("job-history-done")),
                    "Finished history should contain done job");
            assertTrue(cancelledJobs.stream().anyMatch(job -> job.getJobId().equals("job-history-cancelled")),
                    "Cancelled history should contain cancelled job");

            assertEquals(JobScheduler.JobState.DONE, scheduler.getJobStatus("job-history-done"));
            assertEquals(JobScheduler.JobState.CANCELLED, scheduler.getJobStatus("job-history-cancelled"));

            assertTrue(scheduler.getJobsByState(JobScheduler.JobState.DONE)
                    .stream()
                    .anyMatch(job -> job.getJobId().equals("job-history-done")));
            assertTrue(scheduler.getJobsByState(JobScheduler.JobState.CANCELLED)
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

            assertEquals(JobScheduler.JobState.ERROR, scheduler.getJobStatus("job-history-error"));
            assertTrue(scheduler.getErroredJobs().stream().anyMatch(job -> job.getJobId().equals("job-history-error")
                    && "boom".equals(job.getErrorMessage())),
                    "Errored history should contain failed job");
            assertTrue(scheduler.getJobsByState(JobScheduler.JobState.ERROR)
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

            final JobScheduler.JobSnapshot runningSnapshot = scheduler.getAllJobsInQueue()
                    .stream()
                    .filter(job -> job.getJobId().equals("job-process"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(runningSnapshot.getProcessId() != null && runningSnapshot.getProcessId() > 0,
                    "Running snapshot should expose process id");

            assertTrue(handle.cancel(), "Process-backed job should be cancellable");
            waitUntil(handle::isDone, Duration.ofSeconds(5));

            final JobScheduler.JobSnapshot cancelledSnapshot = scheduler.getCancelledJobs()
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

            assertTrue(ProcessHandle.of(childPid.get()).map(ProcessHandle::isAlive).orElse(false),
                    "Spawned child process should be alive before cancellation");

            assertTrue(handle.cancel(), "Process tree job should be cancellable");
            waitUntil(handle::isDone, Duration.ofSeconds(5));

            assertTrue(processStopped.get(), "Root process should no longer be alive after cancellation");
            waitUntil(() -> !ProcessHandle.of(childPid.get()).map(ProcessHandle::isAlive).orElse(false),
                    Duration.ofSeconds(2));
            assertFalse(ProcessHandle.of(childPid.get()).map(ProcessHandle::isAlive).orElse(false),
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
}
