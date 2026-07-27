package org.openscience.sherlock.dbservice.statistics.service;

import org.openscience.sherlock.dbservice.statistics.controller.model.HOSEReplaceAllJobStatus;
import org.openscience.sherlock.utils.IdGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.Disposable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class HOSEReplaceAllJobService {

    private static final int HISTORY_LIMIT = 20;

    private final Map<String, MutableJobStatus> jobsById = new ConcurrentHashMap<>();
    private final AtomicReference<String> activeJobId = new AtomicReference<>();

    public HOSEReplaceAllJobStatus startJob(final String[] nuclei, final int maxSphere, final int batchSize) {
        final String currentActiveJobId = this.activeJobId.get();
        if (currentActiveJobId != null) {
            final MutableJobStatus active = this.jobsById.get(currentActiveJobId);
            if (active != null && active.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A HOSE replaceAll job is already running");
            }
            this.activeJobId.compareAndSet(currentActiveJobId, null);
        }

        final String jobId = IdGenerator.generateId();
        final MutableJobStatus job = MutableJobStatus.queued(jobId, nuclei, maxSphere, batchSize);
        this.jobsById.put(jobId, job);
        this.activeJobId.set(jobId);
        this.trimHistory();
        return job.toSnapshot();
    }

    public void attachSubscription(final String jobId, final Disposable disposable) {
        this.getJobOrThrow(jobId).setDisposable(disposable);
    }

    public void markRunning(final String jobId) {
        this.getJobOrThrow(jobId).markRunning();
    }

    public void onBatchProcessed(final String jobId, final int batchSize) {
        this.getJobOrThrow(jobId).onBatchProcessed(batchSize);
    }

    public void markCompleted(final String jobId) {
        final MutableJobStatus job = this.getJobOrThrow(jobId);
        job.markCompleted();
        this.clearActive(jobId);
        this.trimHistory();
    }

    public void markFailed(final String jobId, final Throwable error) {
        final MutableJobStatus job = this.getJobOrThrow(jobId);
        job.markFailed(error);
        this.clearActive(jobId);
        this.trimHistory();
    }

    public void markCancelled(final String jobId) {
        final MutableJobStatus job = this.getJobOrThrow(jobId);
        job.markCancelled();
        this.clearActive(jobId);
        this.trimHistory();
    }

    public HOSEReplaceAllJobStatus cancelActiveJob() {
        final String jobId = this.activeJobId.get();
        if (jobId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active HOSE replaceAll job");
        }
        final MutableJobStatus job = this.getJobOrThrow(jobId);
        if (!job.isActive()) {
            return job.toSnapshot();
        }

        final Disposable disposable = job.getDisposable();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        job.markCancelled();
        this.clearActive(jobId);
        this.trimHistory();
        return job.toSnapshot();
    }

    public Optional<HOSEReplaceAllJobStatus> getCurrentJobStatus() {
        final String jobId = this.activeJobId.get();
        if (jobId != null) {
            final MutableJobStatus activeJob = this.jobsById.get(jobId);
            if (activeJob != null) {
                return Optional.of(activeJob.toSnapshot());
            }
        }

        return this.jobsById.values().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .findFirst()
                .map(job -> job.toSnapshot());
    }

    public String getActiveJobIdOrThrow() {
        final String jobId = this.activeJobId.get();
        if (jobId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Active HOSE replaceAll job tracking is unavailable");
        }
        return jobId;
    }

    private MutableJobStatus getJobOrThrow(final String jobId) {
        final MutableJobStatus job = this.jobsById.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown HOSE replaceAll jobId: " + jobId);
        }
        return job;
    }

    private void clearActive(final String jobId) {
        this.activeJobId.compareAndSet(jobId, null);
    }

    private void trimHistory() {
        if (this.jobsById.size() <= HISTORY_LIMIT) {
            return;
        }
        final List<MutableJobStatus> removable = new ArrayList<>(this.jobsById.values().stream()
                .filter(job -> !job.isActive())
            .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                .toList());

        int excess = this.jobsById.size() - HISTORY_LIMIT;
        for (final MutableJobStatus status : removable) {
            if (excess <= 0) {
                break;
            }
            if (this.jobsById.remove(status.getJobId(), status)) {
                excess--;
            }
        }
    }

    private static final class MutableJobStatus {

        private final String jobId;
        private final List<String> nuclei;
        private final int maxSphere;
        private final int batchSize;
        private final Instant createdAt;

        private final AtomicLong datasetsProcessed = new AtomicLong(0);
        private final AtomicInteger batchesProcessed = new AtomicInteger(0);

        private volatile String status;
        private volatile Instant startedAt;
        private volatile Instant finishedAt;
        private volatile String message;
        private volatile String errorType;
        private volatile String errorMessage;
        private volatile Disposable disposable;

        private MutableJobStatus(final String jobId, final List<String> nuclei, final int maxSphere, final int batchSize,
                final Instant createdAt) {
            this.jobId = jobId;
            this.nuclei = nuclei;
            this.maxSphere = maxSphere;
            this.batchSize = batchSize;
            this.createdAt = createdAt;
            this.status = "QUEUED";
            this.message = "Job queued";
        }

        static MutableJobStatus queued(final String jobId, final String[] nuclei, final int maxSphere,
                final int batchSize) {
            final List<String> nucleiCopy = List.of(nuclei.clone());
            return new MutableJobStatus(jobId, nucleiCopy, maxSphere, batchSize, Instant.now());
        }

        synchronized void setDisposable(final Disposable disposable) {
            this.disposable = disposable;
        }

        synchronized Disposable getDisposable() {
            return this.disposable;
        }

        synchronized void markRunning() {
            if (!this.isActive()) {
                return;
            }
            this.status = "RUNNING";
            if (this.startedAt == null) {
                this.startedAt = Instant.now();
            }
            this.message = "Rebuild in progress";
        }

        synchronized void onBatchProcessed(final int processedInBatch) {
            this.datasetsProcessed.addAndGet(processedInBatch);
            this.batchesProcessed.incrementAndGet();
            if (this.isActive()) {
                this.status = "RUNNING";
                this.message = "Processed " + this.datasetsProcessed.get() + " datasets";
            }
        }

        synchronized void markCompleted() {
            this.status = "COMPLETED";
            if (this.startedAt == null) {
                this.startedAt = this.createdAt;
            }
            this.finishedAt = Instant.now();
            this.message = "Rebuild completed successfully";
            this.errorType = null;
            this.errorMessage = null;
        }

        synchronized void markFailed(final Throwable error) {
            this.status = "FAILED";
            if (this.startedAt == null) {
                this.startedAt = this.createdAt;
            }
            this.finishedAt = Instant.now();
            this.errorType = error == null ? null : error.getClass().getSimpleName();
            this.errorMessage = error == null ? null : error.getMessage();
            this.message = "Rebuild failed";
        }

        synchronized void markCancelled() {
            this.status = "CANCELLED";
            if (this.startedAt == null) {
                this.startedAt = this.createdAt;
            }
            this.finishedAt = Instant.now();
            this.message = "Rebuild cancelled";
        }

        synchronized boolean isActive() {
            return "QUEUED".equals(this.status) || "RUNNING".equals(this.status);
        }

        synchronized HOSEReplaceAllJobStatus toSnapshot() {
            return new HOSEReplaceAllJobStatus(
                    this.status,
                    this.nuclei,
                    this.maxSphere,
                    this.batchSize,
                    this.datasetsProcessed.get(),
                    this.batchesProcessed.get(),
                    this.createdAt,
                    this.startedAt,
                    this.finishedAt,
                    this.message,
                    this.errorType,
                    this.errorMessage,
                    this.isActive());
        }

        String getJobId() {
            return this.jobId;
        }

        Instant getCreatedAt() {
            return this.createdAt;
        }
    }
}
