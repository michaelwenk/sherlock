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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class FragmentServiceImplementation
        implements FragmentService {

    private final FragmentRepository fragmentRepository;

    public FragmentServiceImplementation(final FragmentRepository fragmentRepository) {
        this.fragmentRepository = fragmentRepository;
    }

    @Override
    public Mono<Long> count() {
        return this.fragmentRepository.count();
    }

    @Override
    public Flux<FragmentRecord> findAll() {
        return this.fragmentRepository.findAll();
    }

    @Override
    public Flux<FragmentRecord> findAllById(final Iterable<String> ids) {
        return this.fragmentRepository.findAllById(ids);
    }


    @Override
    public Mono<FragmentRecord> findById(final String id) {
        return this.fragmentRepository.findById(id);
    }

    @Override
    public Flux<FragmentRecord> findByDataSetSpectrumNuclei(final String[] nuclei) {
        return this.fragmentRepository.findByDataSetSpectrumNuclei(nuclei);
    }

    @Override
    public Flux<FragmentRecord> getByDataSetByNucleiAndSignalCountAndSetBits(final String[] nuclei,
                                                                             final int signalCount, final int[] bits) {
        return this.fragmentRepository.getByDataSetByNucleiAndSignalCountAndSetBits(nuclei, signalCount, bits);
    }

    // insertions/deletions

    @Override
    public Mono<FragmentRecord> insert(final FragmentRecord fragmentRecord) {
        return this.fragmentRepository.insert(fragmentRecord);
    }

    @Override
    public Flux<FragmentRecord> insertMany(final Flux<FragmentRecord> fragmentRecordFlux) {
        return this.fragmentRepository.insert(fragmentRecordFlux);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.fragmentRepository.deleteAll();
    }
}
