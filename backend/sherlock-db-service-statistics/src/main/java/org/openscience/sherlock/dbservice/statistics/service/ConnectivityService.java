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

import org.openscience.sherlock.dbservice.statistics.service.model.ConnectivityRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConnectivityService {

    Mono<Long> count();

    Mono<ConnectivityRecord> insert(final ConnectivityRecord connectivityRecord);

    Flux<ConnectivityRecord> findAll();

    Mono<ConnectivityRecord> findById(final String id);

    Flux<ConnectivityRecord> findByNucleusAndHybridizationAndMultiplicityAndShift(final String nucleus,
                                                                                  final String hybridization,
                                                                                  final String multiplicity,
                                                                                  final int minShift,
                                                                                  final int maxShift);

    Mono<Void> deleteAll();
}
