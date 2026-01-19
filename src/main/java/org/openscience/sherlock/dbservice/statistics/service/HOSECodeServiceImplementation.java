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

import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class HOSECodeServiceImplementation
        implements HOSECodeService {

    @Autowired
    @Qualifier("statisticsMongoTemplate")
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private HOSECodeRepository hoseCodeRepository;

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
    public Mono<HOSECodeRecord> save(final HOSECodeRecord hoseCodeRecord) {
        return this.hoseCodeRepository.save(hoseCodeRecord);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.hoseCodeRepository.deleteAll();
    }

}
