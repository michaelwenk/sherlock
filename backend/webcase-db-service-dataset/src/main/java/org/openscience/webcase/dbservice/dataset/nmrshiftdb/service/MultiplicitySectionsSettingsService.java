package org.openscience.webcase.dbservice.dataset.nmrshiftdb.service;

import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.MultiplicitySectionsSettingsRecord;
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
