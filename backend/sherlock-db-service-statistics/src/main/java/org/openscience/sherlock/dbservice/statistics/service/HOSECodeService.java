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

import java.util.List;
import java.util.Optional;

public interface HOSECodeService {

    //    Mono<Long> count();
    //
    //    Flux<HOSECodeRecord> findAll();
    //
    //    Mono<HOSECodeRecord> findById(final String id);
    //
    //    Mono<Boolean> existsById(final String id);
    //
    //    // insertions/deletions
    //
    //    Mono<HOSECodeRecord> insert(final HOSECodeRecord hoseCodeRecord);
    //
    //    Flux<HOSECodeRecord> insertMany(final Flux<HOSECodeRecord> hoseCodeRecordFlux);
    //
    //    Mono<HOSECodeRecord> save(final HOSECodeRecord hoseCodeRecord);
    //
    //    Mono<Void> deleteAll();

    long count();

    List<HOSECodeRecord> findAll();

    Optional<HOSECodeRecord> findById(final String id);

    boolean existsById(final String id);

    // insertions/deletions

    HOSECodeRecord insert(final HOSECodeRecord hoseCodeRecord);

    List<HOSECodeRecord> insertMany(final List<HOSECodeRecord> hoseCodeRecordList);

    HOSECodeRecord save(final HOSECodeRecord hoseCodeRecord);

    void deleteAll();
}
