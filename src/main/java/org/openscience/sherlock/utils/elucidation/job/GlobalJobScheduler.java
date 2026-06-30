package org.openscience.sherlock.utils.elucidation.job;

import org.openscience.sherlock.utils.elucidation.job.JobScheduler.JobState;

public final class GlobalJobScheduler {
    private static final int DEFAULT_POOL_SIZE = 4;
    private static final int DEFAULT_QUEUE_SIZE = 1000;

    private static final JobScheduler INSTANCE = new JobScheduler(DEFAULT_POOL_SIZE, DEFAULT_QUEUE_SIZE);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::shutdown));
    }

    private GlobalJobScheduler() {
    }

    public static JobScheduler get() {
        return INSTANCE;
    }

    public static boolean waitUntilCancelled(
            final String jobId,
            final long timeoutMillis,
            final long pollIntervalMillis) throws InterruptedException {

        final long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            final JobState state = INSTANCE.getJobStatus(jobId);

            if (state == JobState.CANCELLED) {
                return true;
            }
            if (state == JobState.DONE) {
                return false; // finished before cancellation won
            }
            if (state == JobState.ERROR) {
                return false; // job failed before cancellation completed
            }

            Thread.sleep(pollIntervalMillis);
        }

        return false; // timeout
    }
}
