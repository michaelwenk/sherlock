package org.openscience.sherlock.dbservice.dataset.db.service.mongo;

import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MultiplicitySectionsSettingsServiceImplementation
        implements MultiplicitySectionsSettingsService {

    @Autowired
    @Qualifier("datasetMongoTemplate")
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private MultiplicitySectionsSettingsRepository multiplicitySectionsSettingsRepository;

    @Override
    public Mono<Long> count() {
        return this.multiplicitySectionsSettingsRepository.count();
    }

    @Override
    public Flux<MultiplicitySectionsSettingsRecord> findAll() {
        return this.multiplicitySectionsSettingsRepository.findAll();
    }

    @Override
    public Mono<MultiplicitySectionsSettingsRecord> findById(final String id) {
        return this.multiplicitySectionsSettingsRepository.findById(id);
    }

    @Override
    public Mono<MultiplicitySectionsSettingsRecord> findByNucleus(final String nucleus) {
        return this.multiplicitySectionsSettingsRepository.findByNucleus(nucleus);
    }

    @Override
    public Mono<MultiplicitySectionsSettingsRecord> insert(
            final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord) {
        return this.multiplicitySectionsSettingsRepository.insert(multiplicitySectionsSettingsRecord);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.multiplicitySectionsSettingsRepository.deleteAll();
    }
}
