package org.openscience.webcase.ranking.spectralsimilarity.utils;


import org.openscience.webcase.ranking.spectralsimilarity.model.Assignment;
import org.openscience.webcase.ranking.spectralsimilarity.model.Signal;
import org.openscience.webcase.ranking.spectralsimilarity.model.Spectrum;

import java.util.*;

public class Match {

    /**
     * Checks whether two spectra contain given dimensions.
     *
     * @param spectrum1 first spectrum
     * @param spectrum2 second spectrum
     * @param dim1      dimension to select in first spectrum
     * @param dim2      dimension to select in second spectrum
     *
     * @return true if both spectra contain the selected dimension
     */
    private static boolean checkDimensions(final Spectrum spectrum1, final Spectrum spectrum2, final int dim1, final int dim2) {
        return dim1 < spectrum1.getNuclei().length && dim2 < spectrum2.getNuclei().length;
    }

    /**
     * Returns the closest shift matches between two spectra in selected dimensions
     * as an Assignment object with one set dimension only. <br>
     * Despite intensities are expected, they are still not considered here.
     *
     * @param spectrum      first spectrum
     * @param querySpectrum query spectrum (Subspectrum)
     * @param dim1          dimension in first spectrum to take the shifts from
     * @param dim2          dimension in second spectrum to take the shifts from
     * @param shiftTol      Tolerance value [ppm] used during spectra shift
     *                      comparison
     *
     * @return Assignments with signal indices of spectrum and matched indices
     * in query spectrum; null if one of the spectra does not
     * contain the selected dimension
     */
    public static Assignment matchSpectra(final Spectrum spectrum, final Spectrum querySpectrum, final int dim1, final int dim2, final double shiftTol) {
        if (!Match.checkDimensions(spectrum, querySpectrum, dim1, dim2)) {
            return null;
        }
        final Assignment matchAssignments = new Assignment(spectrum);
        final Set<Integer> assigned = new HashSet<>();
        List<Integer> pickedSignalIndicesSpectrum2;

        for (int i = 0; i < spectrum.getSignalCount(); i++) {
            if (spectrum.getShift(i, dim1) == null)
                continue;

            // @TODO add solvent deviation value for picking closest signal(s)
            pickedSignalIndicesSpectrum2 = new ArrayList<>();
            for (final int pickedSignalIndexSpectrum2 : querySpectrum.pickClosestSignal(spectrum.getShift(i, dim1), dim2, shiftTol)) {
                // @TODO maybe consider further parameters to check ? e.g. intensity
                if (querySpectrum.getMultiplicity(pickedSignalIndexSpectrum2).equals(spectrum.getMultiplicity(i)) && querySpectrum.getEquivalencesCount(pickedSignalIndexSpectrum2) <= spectrum.getEquivalencesCount(i)) {
                    pickedSignalIndicesSpectrum2.add(pickedSignalIndexSpectrum2);
                }
            }
            for (final int pickedSignalIndexSpectrum2 : pickedSignalIndicesSpectrum2) {
                if (!assigned.contains(pickedSignalIndexSpectrum2)) {
                    // add signal to list of already assigned signals
                    assigned.add(pickedSignalIndexSpectrum2);
                    // set picked signal index in assignment object
                    matchAssignments.setAssignment(0, i, pickedSignalIndexSpectrum2);

                    break;
                }
            }
        }

        return matchAssignments;
    }

    /**
     * Returns deviations between matched shifts of two spectra.
     * The matching procedure is already included here.
     *
     * @param spectrum1 first spectrum
     * @param spectrum2 second spectrum
     * @param dim1      dimension in first spectrum to take the shifts from
     * @param dim2      dimension in second spectrum to take the shifts from
     * @param shiftTol
     *
     * @return
     *
     * @see #matchSpectra(Spectrum, Spectrum, int, int, double)
     */
    public static Double[] getDeviations(final Spectrum spectrum1, final Spectrum spectrum2, final int dim1, final int dim2, final double shiftTol) {
        final Double[] deviations = new Double[spectrum1.getSignalCount()];
        final Assignment matchAssignments = Match.matchSpectra(spectrum1, spectrum2, dim1, dim2, shiftTol);
        Signal matchedSignalInSpectrum2;
        for (int i = 0; i < spectrum1.getSignalCount(); i++) {
            if (matchAssignments.getAssignment(0, i) == -1) {
                deviations[i] = null;
            } else {
                matchedSignalInSpectrum2 = spectrum2.getSignal(matchAssignments.getAssignment(0, i));
                deviations[i] = Math.abs(spectrum1.getSignal(i).getShift(dim1) - matchedSignalInSpectrum2.getShift(dim2));
            }
        }
        return deviations;
    }

    /**
     * Returns the average of all deviations within a given input array.
     *
     * @param deviations array of deviations
     *
     * @return
     */
    public static Double calculateAverageDeviation(final Double[] deviations) {
        // every signal has to have a match
        for (final Double deviation : deviations) {
            if (deviation == null) {
                return null;
            }
        }

        return Match.getMean(deviations);
    }

    /**
     * Returns the average of all deviations of matched shifts between two
     * spectra.
     *
     * @param spectrum1 first spectrum
     * @param spectrum2 second spectrum
     * @param dim1      dimension in first spectrum to take the shifts from
     * @param dim2      dimension in second spectrum to take the shifts from
     * @param shiftTol  Tolerance value [ppm] used during peak picking in
     *                  shift comparison
     *
     * @return
     *
     * @see #getDeviations(Spectrum, Spectrum, int, int, double)
     * @see #calculateAverageDeviation(Double[])
     */
    public static Double calculateAverageDeviation(final Spectrum spectrum1, final Spectrum spectrum2, final int dim1, final int dim2, final double shiftTol) {
        return Match.calculateAverageDeviation(Match.getDeviations(spectrum1, spectrum2, dim1, dim2, shiftTol));
    }

    /**
     * @param data
     *
     * @return
     */
    public static Double getMean(final Double[] data) {
        if ((data == null) || (data.length == 0)) {
            return null;
        }
        double sum = 0;
        int nullCounter = 0;
        for (final Double d : data) {
            if (d != null) {
                sum += d;
            } else {
                nullCounter++;
            }
        }
        return ((data.length - nullCounter) != 0) ? (sum / (data.length - nullCounter)) : null;
    }
}
