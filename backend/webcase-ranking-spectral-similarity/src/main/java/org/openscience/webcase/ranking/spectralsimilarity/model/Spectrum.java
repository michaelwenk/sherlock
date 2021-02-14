/*
 * This class was adopted and modified from an earlier version by Christoph Steinbeck
 */


/*
 * The MIT License
 *
 * Copyright 2018 Michael Wenk [https://github.com/michaelwenk].
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.openscience.webcase.ranking.spectralsimilarity.model;


import lombok.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michael Wenk [https://github.com/michaelwenk]
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Spectrum {

    private String[] nuclei;

    private String description;
    private String specType;
    private Double spectrometerFrequency;
    private String solvent;
    private String standard;
    private List<Signal> signals;
    private int signalCount;

    public int getSignalCountWithEquivalences() {
        int sum = 0;
        for (final Signal signal : this.getSignals()) {
            sum += 1 + signal.getEquivalencesCount();
        }
        return sum;
    }

    private boolean checkSignalIndex(final int signalIndex) {
        return (signalIndex >= 0) && (signalIndex < this.getSignalCount());
    }

    public Signal getSignal(final int signalIndex) {
        if (!this.checkSignalIndex(signalIndex)) {
            return null;
        }

        return this.signals.get(signalIndex);
    }

    public Double getShift(final int signalIndex, final int dim) {
        if (!this.checkSignalIndex(signalIndex)) {
            return null;
        }

        return this.getSignal(signalIndex).getShift(dim);
    }

    public String getMultiplicity(final int signalIndex) {
        if (!this.checkSignalIndex(signalIndex)) {
            return null;
        }

        return this.getSignal(signalIndex).getMultiplicity();
    }

    public Boolean hasEquivalences(final int signalIndex) {
        if (!this.checkSignalIndex(signalIndex)) {
            return null;
        }

        return this.getEquivalencesCount(signalIndex) > 0;
    }

    public Integer getEquivalencesCount(final int signalIndex) {
        if (!this.checkSignalIndex(signalIndex)) {
            return null;
        }

        return this.getSignal(signalIndex).getEquivalencesCount();
    }

    public List<Integer> getEquivalencesCounts() {
        return this.getSignals().stream().map(Signal::getEquivalencesCount).collect(Collectors.toList());
    }

    /**
     * Returns the signal index (or indices) closest to the given shift. If no signal is found within the interval
     * defined by {@code pickPrecision}, an empty list is returned.
     *
     * @param shift         query shift
     * @param dim           dimension in spectrum to look in
     * @param pickPrecision tolerance value for search window
     *
     * @return
     */
    public List<Integer> pickClosestSignal(final double shift, final int dim, final double pickPrecision) {
        final List<Integer> matchIndices = new ArrayList<>();
        if (!this.containsDim(dim)) {
            return matchIndices;
        }
        double minDiff = pickPrecision;
        // detect the minimal difference between a signal shift to the given query shift
        for (int s = 0; s < this.getSignalCount(); s++) {
            if (Math.abs(this.getShift(s, dim) - shift) < minDiff) {
                minDiff = Math.abs(this.getShift(s, dim) - shift);
            }
        }
        for (int s = 0; s < this.getSignalCount(); s++) {
            if (Math.abs(this.getShift(s, dim) - shift) == minDiff) {
                matchIndices.add(s);
            }
        }

        return matchIndices;
    }

    /**
     * Returns a list of signal indices within the interval defined by
     * pickPrecision. That list is sorted by the distances to the query shift.
     * If none is found an empty ArrayList is returned.
     *
     * @param shift         query shift
     * @param dim           dimension in spectrum to look in
     * @param pickPrecision tolerance value for search window
     *
     * @return
     */
    public List<Integer> pickSignals(final Double shift, final int dim, final double pickPrecision) {
        final List<Integer> pickedSignals = new ArrayList<>();
        if (!this.containsDim(dim)) {
            return pickedSignals;
        }
        for (int s = 0; s < this.getSignalCount(); s++) {
            if (Math.abs(this.getShift(s, dim) - shift) <= pickPrecision) {
                pickedSignals.add(s);
            }
        }
        // sort signal indices by distance to query shift
        pickedSignals.sort(Comparator.comparingDouble(pickedSignalIndex -> Math.abs(shift - this.getShift(pickedSignalIndex, dim))));

        return pickedSignals;
    }

    public int getNDim(){
        return this.getNuclei().length;
    }

    public boolean containsDim(final int dim){
        return dim < this.getNDim();
    }
}
