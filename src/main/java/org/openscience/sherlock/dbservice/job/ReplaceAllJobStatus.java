package org.openscience.sherlock.dbservice.job;

import java.time.Instant;
import java.util.Map;

/**
 * Snapshot of a {@code replaceAll} job managed by
 * {@link ReplaceAllJobRegistry}.
 *
 * <p>
 * The record is a generic counterpart to
 * {@link org.openscience.sherlock.dbservice.statistics.controller.model.HOSEReplaceAllJobStatus}
 * that can describe rebuilds of any statistics/fragment collection. The
 * specific
 * inputs of a rebuild (e.g. nuclei array, maximum sphere, batch size) are
 * carried
 * in {@link #parameters()} to keep the status shape identical across kinds.
 * </p>
 */
public record ReplaceAllJobStatus(
        String jobId,
        String kind,
        String status,
        Map<String, Object> parameters,
        long datasetsProcessed,
        int batchesProcessed,
        Map<String, Long> stageProgress,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String message,
        String errorType,
        String errorMessage,
        boolean active) {
}
