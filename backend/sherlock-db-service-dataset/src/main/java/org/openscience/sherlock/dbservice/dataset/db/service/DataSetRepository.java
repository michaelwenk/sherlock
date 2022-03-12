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
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface DataSetRepository
        extends ReactiveMongoRepository<DataSetRecord, String> {

    @Override
    Mono<DataSetRecord> findById(final String id);

    @Override
    Flux<DataSetRecord> findAllById(Iterable<String> iterable);

    @Query(value = "{\"dataSet.meta.mf\": \"?0\"}")
    Flux<DataSetRecord> findByMf(final String mf);

    @Query(value = "{\"dataSet.meta.source\": \"?0\"}")
    Flux<DataSetRecord> findBySource(final String source);

    Flux<DataSetRecord> findByDataSetSpectrumNuclei(final String[] nuclei);

    @Query(value = "{\"dataSet.spectrum.nuclei\": ?0, \"dataSet.meta.source\": \"?1\"}")
    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndSource(final String[] nuclei, final String source);

    @Query(value = "{\"dataSet.spectrum.nuclei\": ?0, \"dataSet.attachment.setBits\": ?1}")
    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndAttachmentSetBits(final String[] nuclei, final int[] setBits);

    @Query(value = "{\"dataSet.spectrum.nuclei\": ?0, \"dataSet.attachment.setBits\": ?1, \"dataSet.meta.mf\": \"?2\"}")
    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(final String[] nuclei, final int[] setBits,
                                                                             final String mf);


    @Query(value = "{\"dataSet.spectrum.nuclei\": ?0, \"dataSet.spectrum.signals\": {$size: ?1}}")
    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(final String[] nuclei,
                                                                                 final int signalCount);

    @Query(value = "{\"dataSet.spectrum.nuclei\": ?0, \"dataSet.spectrum.signals\": {$size: ?1}, \"dataSet.meta.mf\": \"?2\"}")
    Flux<DataSetRecord> findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(final String[] nuclei,
                                                                                      final int signalCount,
                                                                                      final String mf);
}
