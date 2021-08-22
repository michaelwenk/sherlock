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

package org.openscience.webcase.dbservicehybridization.service;

import org.openscience.webcase.dbservicehybridization.service.model.HybridizationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class HybridizationServiceImplementation
        implements HybridizationService {

    private final HybridizationRepository hybridizationRepository;

    @Autowired
    public HybridizationServiceImplementation(final HybridizationRepository hybridizationRepository) {
        this.hybridizationRepository = hybridizationRepository;
    }

    @Override
    public Mono<Long> count() {
        return this.hybridizationRepository.count();
    }

    @Override
    public Mono<HybridizationRecord> insert(final HybridizationRecord hybridizationRecord) {
        return this.hybridizationRepository.insert(hybridizationRecord);
    }

    @Override
    public Flux<HybridizationRecord> findAll() {
        return this.hybridizationRepository.findAll();
    }

    @Override
    public Mono<HybridizationRecord> findById(final String id) {
        return this.hybridizationRepository.findById(id);
    }

    @Override
    public Flux<String> aggregateHybridizationsByNucleusAndShiftAndMultiplicity(final String nucleus,
                                                                                final int minShift, final int maxShift,
                                                                                final String multiplicity) {
        return this.hybridizationRepository.aggregateHybridizationsByNucleusAndShiftAndMultiplicity(nucleus, minShift,
                                                                                                    maxShift,
                                                                                                    multiplicity);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.hybridizationRepository.deleteAll();
    }
}
