package org.openscience.sherlock.dbservice.job;

import org.openscience.sherlock.utils.IdGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.Disposable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic registry for asynchronous {@code replaceAll} rebuild jobs.
 *
 * <p>
 * Each rebuild kind (e.g. {@code "connectivity"}, {@code "hybridization"},
 * {@code "heavyAtom"}, {@code "fragment"}) may have at most one active job at a
 * time; attempting to start a second one raises HTTP 409. A limited history of
 * finished jobs is retained per kind for status polling.
 * </p>
 */
@Service
public class ReplaceAllJobRegistry {

    private static final int HISTORY_LIMIT_PER_KIND = 20;

    private final Map<String, MutableJobStatus> jobsById = new ConcurrentHashMap<>();
    private final Map<String, String> activeJobIdByKind = new ConcurrentHashMap<>();

    public ReplaceAllJobStatus startJob(final String kind, final Map<String, Object> parameters) {
        Objects.requireNonNull(kind, "kind");
        final String existingActive = this.activeJobIdByKind.get(kind);
        if (existingActive != null) {
            final MutableJobStatus active = this.jobsById.get(existingActive);
            if (active != null && active.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A " + kind + " replaceAll job is already running");
            }
            this.activeJobIdByKind.remove(kind, existingActive);
        }

        final String jobId = IdGenerator.generateId();
        final MutableJobStatus job = new MutableJobStatus(jobId, kind, parameters == null
                ? Map.of()
                : new LinkedHashMap<>(parameters));
        this.jobsById.put(jobId, job);
        this.activeJobIdByKind.put(kind, jobId);
        this.trimHistory(kind);
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

    /**
     * Reports progress for a specific stage of a composite job. The per-stage
     * counter is
     * incremented independently, while {@code datasetsProcessed} is kept as the
     * maximum
     * across all stage counters so that it always reflects the actual number of
     * distinct
     * {@code DataSet} objects processed by the pipeline (never inflated by
     * sub-stages that
     * re-process the same datasets).
     */
    public void onStageBatchProcessed(final String jobId, final String stage, final int batchSize) {
        this.getJobOrThrow(jobId).onStageBatchProcessed(stage, batchSize);
    }

    public void markCompleted(final String jobId) {
        final MutableJobStatus job = this.getJobOrThrow(jobId);
        job.markCompleted();
        this.clearActive(job);
        this.trimHistory(job.getKind());
    }

    public void markFailed(final String jobId, final Throwable error) {
        final MutableJobStatus job = this.getJobOrThrow(jobId);
        job.markFailed(error);
        this.clearActive(job);
        this.trimHistory(job.getKind());
    }

    public void markCancelled(final String jobId) {
        final MutableJobStatus job = this.getJobOrThrow(jobId);
        job.markCancelled();
        this.clearActive(job);
        this.trimHistory(job.getKind());
    }

    public Optional<ReplaceAllJobStatus> getCurrentJobStatus(final String kind) {
        final String jobId = this.activeJobIdByKind.get(kind);
        if (jobId != null) {
            final MutableJobStatus activeJob = this.jobsById.get(jobId);
            if (activeJob != null) {
                return Optional.of(activeJob.toSnapshot());
            }
        }

        return this.jobsById.values().stream()
                .filter(job -> kind.equals(job.getKind()))
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .findFirst()
                .map(MutableJobStatus::toSnapshot);
    }

    public String getActiveJobIdOrThrow(final String kind) {
        final String jobId = this.activeJobIdByKind.get(kind);
        if (jobId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Active " + kind + " replaceAll job tracking is unavailable");
        }
        return jobId;
    }

    private MutableJobStatus getJobOrThrow(final String jobId) {
        final MutableJobStatus job = this.jobsById.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Unknown replaceAll jobId: " + jobId);
        }
        return job;
    }

    private void clearActive(final MutableJobStatus job) {
        this.activeJobIdByKind.remove(job.getKind(), job.getJobId());
    }

    private void trimHistory(final String kind) {
        final List<MutableJobStatus> forKind = this.jobsById.values().stream()
                .filter(job -> kind.equals(job.getKind()))
                .toList();
        if (forKind.size() <= HISTORY_LIMIT_PER_KIND) {
            return;
        }
        final List<MutableJobStatus> removable = new ArrayList<>(forKind.stream()
                .filter(job -> !job.isActive())
                .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                .toList());

        int excess = forKind.size() - HISTORY_LIMIT_PER_KIND;
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
        private final String kind;
        private final Map<String, Object> parameters;
        private final Instant createdAt;

        private final AtomicLong datasetsProcessed = new AtomicLong(0);
        private final AtomicInteger batchesProcessed = new AtomicInteger(0);
        private final Map<String, AtomicLong> stageCounters = new ConcurrentHashMap<>();

        private volatile String status;
        private volatile Instant startedAt;
        private volatile Instant finishedAt;
        private volatile String message;
        private volatile String errorType;
        private volatile String errorMessage;
        private volatile Disposable disposable;

        MutableJobStatus(final String jobId, final String kind, final Map<String, Object> parameters) {
            this.jobId = jobId;
            this.kind = kind;
            this.parameters = parameters;
            this.createdAt = Instant.now();
            this.status = "QUEUED";
            this.message = "Job queued";
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

        synchronized void onStageBatchProcessed(final String stage, final int processedInBatch) {
            final AtomicLong stageCounter = this.stageCounters.computeIfAbsent(
                    Objects.requireNonNull(stage, "stage"),
                    unused -> new AtomicLong(0));
            final long stageTotal = stageCounter.addAndGet(processedInBatch);
            this.batchesProcessed.incrementAndGet();
            // datasetsProcessed reflects the actual number of distinct DataSet objects
            // handled by the pipeline. Since every stage runs against the same underlying
            // dataset set, we take the maximum across stages rather than summing.
            long maxAcrossStages = stageTotal;
            for (final AtomicLong counter : this.stageCounters.values()) {
                final long current = counter.get();
                if (current > maxAcrossStages) {
                    maxAcrossStages = current;
                }
            }
            this.datasetsProcessed.set(maxAcrossStages);
            if (this.isActive()) {
                this.status = "RUNNING";
                this.message = "Processed " + maxAcrossStages + " datasets (" + stage + ": " + stageTotal + ")";
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

        synchronized ReplaceAllJobStatus toSnapshot() {
            final Map<String, Long> stageSnapshot = new LinkedHashMap<>();
            for (final Map.Entry<String, AtomicLong> entry : this.stageCounters.entrySet()) {
                stageSnapshot.put(entry.getKey(), entry.getValue().get());
            }
            return new ReplaceAllJobStatus(
                    this.jobId,
                    this.kind,
                    this.status,
                    this.parameters,
                    this.datasetsProcessed.get(),
                    this.batchesProcessed.get(),
                    stageSnapshot,
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

        String getKind() {
            return this.kind;
        }

        Instant getCreatedAt() {
            return this.createdAt;
        }
    }
}
