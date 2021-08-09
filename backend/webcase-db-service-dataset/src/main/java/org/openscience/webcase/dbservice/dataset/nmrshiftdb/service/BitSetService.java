package org.openscience.webcase.dbservice.dataset.nmrshiftdb.service;

import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.BitSetRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BitSetService {

    Mono<Long> count();

    Flux<BitSetRecord> findAll();

    Mono<BitSetRecord> findById(final String id);

    Flux<BitSetRecord> findBitSetRecordByNucleusAndFingerprintSizeAndSetBitsCount(final String nucleus,
                                                                                  final long fingerprintSize,
                                                                                  final int setBitsCount);

    Flux<BitSetRecord> findBitSetRecordByNucleusAndFingerprintSizeAndSetBits(final String nucleus,
                                                                             final long fingerprintSize,
                                                                             final int[] setBits);

    // insertions/deletions

    Mono<BitSetRecord> insert(final BitSetRecord bitSetRecord);

    Mono<Void> deleteAll();
}
