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

package org.openscience.sherlock.dbservice.dataset.db.service.mongo;

import org.openscience.sherlock.dbservice.dataset.config.DatasetMongoConfig;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DataSetServiceImplementation
        implements DataSetService {

    @Autowired
    @Qualifier("datasetMongoTemplate")
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private DataSetRepository dataSetRepository;

    @Override
    public Mono<Long> count() {
        return this.dataSetRepository.count();
    }

    @Override
    public Flux<DataSetRecord> findAll() {
        return this.dataSetRepository.findAll();
    }

    @Override
    public Mono<DataSetRecord> findById(final String id) {
        return this.dataSetRepository.findById(id);
    }

    @Override
    public Flux<DataSetRecord> findAllById(final Iterable<String> iterable) {
        return this.dataSetRepository.findAllById(iterable);
    }

    @Override
    public Flux<DataSetRecord> findByMf(final String mf) {
        return this.dataSetRepository.findByMf(mf);
    }

    @Override
    public Flux<DataSetRecord> findBySource(final String source) {
        return this.dataSetRepository.findBySource(source);
    }

    @Override
    public Flux<DataSetRecord> findByDataSetSpectrumNuclei(final String[] nuclei) {
        return this.dataSetRepository.findByDataSetSpectrumNuclei(nuclei);
    }

    @Override
    public Flux<DataSetRecord> findByDataSetSpectrumNucleiAndSource(final String[] nuclei, final String source) {
        return this.dataSetRepository.findByDataSetSpectrumNucleiAndSource(nuclei, source);
    }

    @Override
    public Flux<DataSetRecord> findByDataSetSpectrumNucleiAndAttachmentSetBits(final String[] nuclei,
            final int[] setBits) {
        return this.dataSetRepository.findByDataSetSpectrumNucleiAndAttachmentSetBits(nuclei, setBits);
    }

    @Override
    public Flux<DataSetRecord> findByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(final String[] nuclei,
            final int[] setBits,
            final String mf) {
        return this.dataSetRepository.findByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(nuclei, setBits, mf);
    }

    @Override
    public Flux<DataSetRecord> findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(final String[] nuclei,
            final int signalCount) {
        return this.dataSetRepository.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(nuclei, signalCount);
    }

    @Override
    public Flux<DataSetRecord> findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(final String[] nuclei,
            final int signalCount,
            final String mf) {
        return this.dataSetRepository.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(nuclei, signalCount,
                mf);
    }

    // insertions/deletions

    @Override
    public Mono<DataSetRecord> insert(final DataSetRecord dataSetRecord) {
        return this.dataSetRepository.insert(dataSetRecord);
    }

    @Override
    public Flux<DataSetRecord> insertMany(final Flux<DataSetRecord> dataSetRecordFlux) {
        return this.dataSetRepository.insert(dataSetRecordFlux);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.dataSetRepository.deleteAll();
    }

    @Override
    public Mono<Void> deleteById(final String id) {
        return this.dataSetRepository.deleteById(id);
    }

    @Override
    public Mono<Void> updateIndexes() {
        System.out.println("-> updating indices for dataset collection...");

        System.out.println("-> dropping nuclei index in dataset collection...");
        this.reactiveMongoTemplate.indexOps("datasets").dropIndex(DatasetMongoConfig.DATASET_INDEX_NAME_NUCLEI).block();
        System.out.println("-> dropped nuclei index in dataset collection...");

        System.out.println("-> dropping molecular formula index in dataset collection...");
        this.reactiveMongoTemplate.indexOps("datasets").dropIndex(DatasetMongoConfig.DATASET_INDEX_NAME_MF).block();
        System.out.println("-> dropped molecular formula index in dataset collection...");

        System.out.println("-> creating index for nuclei array...");
        final String nucleiIndexString = this.reactiveMongoTemplate.indexOps("datasets")
                .createIndex(new Index().on("dataSet.spectrum.nuclei", Direction.ASC)
                        .named(DatasetMongoConfig.DATASET_INDEX_NAME_NUCLEI))
                .block();
        System.out.println("-> created nuclei index: " + nucleiIndexString);

        System.out.println("-> creating index for molecular formula...");
        final String mfIndexString = this.reactiveMongoTemplate.indexOps("datasets")
                .createIndex(new Index().on("dataSet.meta.mf", Direction.ASC)
                        .named(DatasetMongoConfig.DATASET_INDEX_NAME_MF))
                .block();
        System.out.println("-> created molecular formula index: " + mfIndexString);

        System.out.println("-> updated indices for dataset collection.");

        return Mono.empty();
    }
}
