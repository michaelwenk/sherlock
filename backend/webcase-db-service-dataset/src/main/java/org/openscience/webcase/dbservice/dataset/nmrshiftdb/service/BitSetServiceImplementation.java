package org.openscience.webcase.dbservice.dataset.nmrshiftdb.service;

import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.BitSetRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BitSetServiceImplementation
        implements BitSetService {

    private final BitSetRepository bitSetRepository;

    public BitSetServiceImplementation(final BitSetRepository bitSetRepository) {
        this.bitSetRepository = bitSetRepository;
    }

    @Override
    public Mono<Long> count() {
        return this.bitSetRepository.count();
    }

    @Override
    public Flux<BitSetRecord> findAll() {
        return this.bitSetRepository.findAll();
    }

    @Override
    public Mono<BitSetRecord> findById(final String id) {
        return this.bitSetRepository.findById(id);
    }

    @Override
    public Flux<BitSetRecord> findBitSetRecordByNucleusAndFingerprintSizeAndSetBitsCount(final String nucleus,
                                                                                         final long fingerprintSize,
                                                                                         final int setBitsCount) {
        return this.bitSetRepository.findBitSetRecordByNucleusAndFingerprintSizeAndSetBitsCount(nucleus,
                                                                                                fingerprintSize,
                                                                                                setBitsCount);
    }

    @Override
    public Flux<BitSetRecord> findBitSetRecordByNucleusAndFingerprintSizeAndSetBits(final String nucleus,
                                                                                    final long fingerprintSize,
                                                                                    final int[] setBits) {
        return this.bitSetRepository.findBitSetRecordByNucleusAndFingerprintSizeAndSetBits(nucleus, fingerprintSize,
                                                                                           setBits);
    }


    // insertions/deletions

    @Override
    public Mono<BitSetRecord> insert(final BitSetRecord bitSetRecord) {
        return this.bitSetRepository.insert(bitSetRecord);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.bitSetRepository.deleteAll();
    }
}
