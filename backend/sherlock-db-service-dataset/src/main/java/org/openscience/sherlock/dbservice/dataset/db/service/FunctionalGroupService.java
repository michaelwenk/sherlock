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

package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.db.model.FunctionalGroupRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FunctionalGroupService {

    Mono<Long> count();

    Flux<FunctionalGroupRecord> findAll();

    Flux<FunctionalGroupRecord> findAllById(Iterable<String> ids);

    Mono<FunctionalGroupRecord> findById(final String id);

    Flux<FunctionalGroupRecord> findByDataSetSpectrumNuclei(final String[] nuclei);

    Flux<FunctionalGroupRecord> getByDataSetByNucleiAndSignalCountAndSetBits(final String[] nuclei,
                                                                             final int signalCount, final int[] bits);

    // insertions/deletions

    Mono<FunctionalGroupRecord> insert(final FunctionalGroupRecord functionalGroupRecord);

    Flux<FunctionalGroupRecord> insertMany(final Flux<FunctionalGroupRecord> functionalGroupRecordFlux);

    Mono<Void> deleteAll();
}
