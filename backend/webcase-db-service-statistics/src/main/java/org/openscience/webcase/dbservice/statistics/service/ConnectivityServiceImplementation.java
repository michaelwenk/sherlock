package org.openscience.webcase.dbservice.statistics.service;

import org.openscience.webcase.dbservice.statistics.service.model.ConnectivityRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ConnectivityServiceImplementation
        implements ConnectivityService {

    private final ConnectivityRepository connectivityRepository;

    @Autowired
    public ConnectivityServiceImplementation(final ConnectivityRepository connectivityRepository) {
        this.connectivityRepository = connectivityRepository;
    }

    @Override
    public Mono<Long> count() {
        return this.connectivityRepository.count();
    }

    @Override
    public Mono<ConnectivityRecord> insert(final ConnectivityRecord connectivityRecord) {
        return this.connectivityRepository.insert(connectivityRecord);
    }

    @Override
    public Flux<ConnectivityRecord> findAll() {
        return this.connectivityRepository.findAll();
    }

    @Override
    public Mono<ConnectivityRecord> findById(final String id) {
        return this.connectivityRepository.findById(id);
    }

    @Override
    public Flux<ConnectivityRecord> findByNucleusAndHybridizationAndMultiplicityAndShift(final String nucleus,
                                                                                         final String hybridization,
                                                                                         final String multiplicity,
                                                                                         final int minShift,
                                                                                         final int maxShift) {
        return this.connectivityRepository.findByNucleusAndHybridizationAndMultiplicityAndShift(nucleus, hybridization,
                                                                                                multiplicity, minShift,
                                                                                                maxShift);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.connectivityRepository.deleteAll();
    }
}
