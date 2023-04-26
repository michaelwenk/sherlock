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

import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataSetService {

    Mono<Long> count();

    Flux<DataSetRecord> findAll();

    Flux<DataSetRecord> findFirstN(final long n);

    Mono<DataSetRecord> findById(final String id);

    Flux<DataSetRecord> findAllById(Iterable<String> iterable);

    Flux<DataSetRecord> findByMf(final String mf);

    Flux<DataSetRecord> findBySource(final String source);

    Flux<DataSetRecord> findByDataSetSpectrumNuclei(final String[] nuclei);

    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndSource(final String[] nuclei, final String source);

    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndAttachmentSetBits(final String[] nuclei, final int[] setBits);

    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(final String[] nuclei, final int[] setBits,
                                                                             final String mf);

    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(final String[] nuclei,
                                                                                 final int signalCount);

    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(final String[] nuclei,
                                                                                      final int signalCount,
                                                                                      final String mf);

    // insertions/deletions

    Mono<DataSetRecord> insert(final DataSetRecord dataSetRecord);

    Flux<DataSetRecord> insertMany(final Flux<DataSetRecord> dataSetRecordFlux);

    Mono<Void> deleteAll();
}
