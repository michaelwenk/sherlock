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


import org.openscience.sherlock.dbservice.dataset.db.model.FragmentRecord;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface FunctionalGroupRepository
        extends ReactiveMongoRepository<FragmentRecord, String> {

    @Override
    Mono<FragmentRecord> findById(final String id);

    @Override
    Flux<FragmentRecord> findAllById(Iterable<String> iterable);

    Flux<FragmentRecord> findByDataSetSpectrumNuclei(final String[] nuclei);

    @Aggregation(pipeline = {"{$match: {\"dataSet.spectrum.nuclei\": ?0}}",
                             "{$project: {dataSet: 1, sizeOfSpectrum: {$size: \"$dataSet.spectrum.signals\"}}}, {$match: {\"sizeOfSpectrum\": {$lt: ?1}}}",
                             "{ $project: { \"dataSet\": 1, isSubset: { $setIsSubset: [ \"$dataSet.attachment.setBits\", ?2]}}}",
                             "{$match: {\"isSubset\": true}}"})
    Flux<FragmentRecord> getByDataSetByNucleiAndSignalCountAndSetBits(final String[] nuclei, final int signalCount,
                                                                      final int[] bits);
}
