package org.openscience.webcase.dbservice.dataset.nmrshiftdb.service;

import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.MultiplicitySectionsSettingsRecord;
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
