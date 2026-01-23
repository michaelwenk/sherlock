package org.openscience.sherlock.dbservice.dataset.db.service.mongo;

import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MultiplicitySectionsSettingsRepository
        extends ReactiveMongoRepository<MultiplicitySectionsSettingsRecord, String> {

    Mono<MultiplicitySectionsSettingsRecord> findById(final String id);

    Flux<MultiplicitySectionsSettingsRecord> findByNucleus(final String nucleus);

    Mono<Void> deleteByNucleus(final String nucleus);
}
