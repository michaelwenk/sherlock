package org.openscience.sherlock.dbservice.statistics.service;

import org.openscience.sherlock.dbservice.statistics.service.model.HeavyAtomStatisticsRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface HeavyAtomStatisticsRepository
        extends ReactiveMongoRepository<HeavyAtomStatisticsRecord, String> {

    @Override
    Mono<HeavyAtomStatisticsRecord> findById(final String id);

    Flux<HeavyAtomStatisticsRecord> findHeavyAtomStatisticsRecordByElementsString(final String elementsString);

    Flux<HeavyAtomStatisticsRecord> findHeavyAtomStatisticsRecordByAtomPair(final String atomPair);
}
