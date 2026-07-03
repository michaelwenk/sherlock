package org.openscience.sherlock.utils.elucidation.job;

import org.openscience.sherlock.utils.elucidation.job.JobScheduler.JobState;

public final class GlobalJobScheduler {
    private static final int DEFAULT_POOL_SIZE = 4;
    private static final int DEFAULT_QUEUE_SIZE = 1000;

    private static final Object LOCK = new Object();
    private static volatile int configuredPoolSize = DEFAULT_POOL_SIZE;
    private static volatile int configuredQueueSize = DEFAULT_QUEUE_SIZE;
    private static volatile JobScheduler instance;
    private static volatile boolean shutdownHookRegistered;

    private GlobalJobScheduler() {
    }

    public static void configure(final int poolSize, final int queueSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be greater than 0");
        }
        if (queueSize <= 0) {
            throw new IllegalArgumentException("queueSize must be greater than 0");
        }

        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("GlobalJobScheduler is already initialized");
            }
            configuredPoolSize = poolSize;
            configuredQueueSize = queueSize;
        }
    }

    public static JobScheduler get() {
        JobScheduler localInstance = instance;
        if (localInstance == null) {
            synchronized (LOCK) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = new JobScheduler(configuredPoolSize, configuredQueueSize);
                    instance = localInstance;
                    registerShutdownHookIfNeeded();
                }
            }
        }
        return localInstance;
    }

    public static boolean waitUntilCancelled(
            final String jobId,
            final long timeoutMillis,
            final long pollIntervalMillis) throws InterruptedException {

        final long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            final JobState state = get().getJobStatus(jobId);

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

    private static void registerShutdownHookIfNeeded() {
        if (shutdownHookRegistered) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            final JobScheduler scheduler = instance;
            if (scheduler != null) {
                scheduler.shutdown();
            }
        }));
        shutdownHookRegistered = true;
    }
}
