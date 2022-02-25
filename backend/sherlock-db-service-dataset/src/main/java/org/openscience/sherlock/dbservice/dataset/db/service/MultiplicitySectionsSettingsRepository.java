package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MultiplicitySectionsSettingsRepository
        extends ReactiveMongoRepository<MultiplicitySectionsSettingsRecord, String> {

    @Override
    Mono<MultiplicitySectionsSettingsRecord> findById(final String id);

    Mono<MultiplicitySectionsSettingsRecord> findByNucleus(String nucleus);
}
