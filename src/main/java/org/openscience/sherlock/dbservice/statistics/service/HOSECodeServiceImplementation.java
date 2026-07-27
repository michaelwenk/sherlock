/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Wenk (https://github.com/michaelwenk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openscience.sherlock.dbservice.statistics.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.ReactiveBulkOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class HOSECodeServiceImplementation
        implements HOSECodeService {

    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final HOSECodeRepository hoseCodeRepository;

    public HOSECodeServiceImplementation(
            @Qualifier("statisticsMongoTemplate") final ReactiveMongoTemplate reactiveMongoTemplate,
            final HOSECodeRepository hoseCodeRepository) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
        this.hoseCodeRepository = hoseCodeRepository;
    }

    @Override
    public Mono<Long> count() {
        return this.hoseCodeRepository.count();
    }

    @Override
    public Flux<HOSECodeRecord> findAll() {
        return this.hoseCodeRepository.findAll();
    }

    @Override
    public Mono<HOSECodeRecord> findById(final String id) {
        return this.hoseCodeRepository.findById(id);
    }

    @Override
    public Mono<Boolean> existsById(final String id) {
        return this.hoseCodeRepository.existsById(id);
    }

    // insertions/deletions

    @Override
    public Mono<HOSECodeRecord> insert(final HOSECodeRecord hoseCodeRecord) {
        return this.hoseCodeRepository.insert(hoseCodeRecord);
    }

    @Override
    public Flux<HOSECodeRecord> insertMany(final Flux<HOSECodeRecord> hoseCodeRecordFlux) {
        return this.hoseCodeRepository.insert(hoseCodeRecordFlux);
    }

    @Override
    public Mono<Void> upsertValuesBulk(final List<HOSECodeRecord> hoseCodeRecords) {
        return Mono.defer(() -> {
            if (hoseCodeRecords.isEmpty()) {
                return Mono.<Void>empty();
            }

            final ReactiveBulkOperations bulkOperations = this.reactiveMongoTemplate
                    .bulkOps(BulkOperations.BulkMode.UNORDERED, HOSECodeRecord.class);

            for (final HOSECodeRecord hoseCodeRecord : hoseCodeRecords) {
                final Query query = Query.query(Criteria.where("_id").is(hoseCodeRecord.getId()));
                final Update update = new Update()
                        .setOnInsert("_id", hoseCodeRecord.getId())
                        .setOnInsert("statistics", new HashMap<String, Double[]>());

                for (final Map.Entry<String, Map<String, Long>> solventEntry : hoseCodeRecord.getValues().entrySet()) {
                    for (final Map.Entry<String, Long> shiftEntry : solventEntry.getValue().entrySet()) {
                        update.inc("values." + solventEntry.getKey() + "." + shiftEntry.getKey(),
                                shiftEntry.getValue());
                    }
                }

                bulkOperations.upsert(query, update);
            }

            return bulkOperations.execute().then();
        });
    }

    @Override
    public Mono<HOSECodeRecord> save(final HOSECodeRecord hoseCodeRecord) {
        return this.hoseCodeRepository.save(hoseCodeRecord);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.hoseCodeRepository.deleteAll();
    }

}
