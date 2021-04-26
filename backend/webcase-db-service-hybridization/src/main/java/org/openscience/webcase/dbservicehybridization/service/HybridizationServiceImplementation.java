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

import java.util.List;
import java.util.Optional;


@Service
public class HybridizationServiceImplementation
        implements HybridizationService {

    private final HybridizationRepository hybridizationRepository;

    @Autowired
    public HybridizationServiceImplementation(final HybridizationRepository hybridizationRepository) {
        this.hybridizationRepository = hybridizationRepository;
    }

    @Override
    public long count() {
        return this.hybridizationRepository.count();
    }

    @Override
    public void insert(final HybridizationRecord hybridizationRecord) {
        this.hybridizationRepository.insert(hybridizationRecord);
    }

    @Override
    public List<HybridizationRecord> findAll() {
        return this.hybridizationRepository.findAll();
    }

    @Override
    public Optional<HybridizationRecord> findById(final String id) {
        return this.hybridizationRepository.findById(id);
    }

    @Override
    public List<String> aggregateHybridizationsByNucleusAndShiftAndMultiplicity(final String nucleus,
                                                                                final int minShift, final int maxShift,
                                                                                final String multiplicity) {
        return this.hybridizationRepository.aggregateHybridizationsByNucleusAndShiftAndMultiplicity(nucleus, minShift,
                                                                                                    maxShift,
                                                                                                    multiplicity);
    }

    @Override
    public void deleteAll() {
        this.hybridizationRepository.deleteAll();
    }
}
