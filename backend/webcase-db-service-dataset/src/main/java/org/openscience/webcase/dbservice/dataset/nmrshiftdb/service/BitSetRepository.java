package org.openscience.webcase.dbservice.dataset.nmrshiftdb.service;

import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.BitSetRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BitSetRepository
        extends ReactiveMongoRepository<BitSetRecord, String> {

    @Override
    Mono<BitSetRecord> findById(final String id);

    Flux<BitSetRecord> findBitSetRecordByNucleusAndFingerprintSizeAndSetBitsCount(final String nucleus,
                                                                                  final long fingerprintSize,
                                                                                  final int setBitsCount);

    Flux<BitSetRecord> findBitSetRecordByNucleusAndFingerprintSizeAndSetBits(final String nucleus,
                                                                             final long fingerprintSize,
                                                                             final int[] setBits);
}
