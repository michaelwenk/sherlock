package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MultiplicitySectionsSettingsService {

    Mono<Long> count();

    Flux<MultiplicitySectionsSettingsRecord> findAll();

    Mono<MultiplicitySectionsSettingsRecord> findById(final String id);

    Mono<MultiplicitySectionsSettingsRecord> findByNucleus(final String nucleus);

    // insertions/deletions

    Mono<MultiplicitySectionsSettingsRecord> insert(
            final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord);

    Mono<Void> deleteAll();
}
