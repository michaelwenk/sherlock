package org.openscience.sherlock.dbservice.statistics.controller.model;

import java.time.Instant;
import java.util.List;

public record HOSEReplaceAllJobStatus(
        String status,
        List<String> nuclei,
        int maxSphere,
        int batchSize,
        long datasetsProcessed,
        int batchesProcessed,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String message,
        String errorType,
        String errorMessage,
        boolean active) {
}
