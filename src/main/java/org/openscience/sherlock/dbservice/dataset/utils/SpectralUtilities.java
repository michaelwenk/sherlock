package org.openscience.sherlock.dbservice.dataset.utils;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.SpectrumCompact;
import casekit.nmr.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class SpectralUtilities {

    public static boolean checkShifts(final DataSet dataSet, final int minShift, final int maxShift) {
        return Arrays.stream(dataSet.getSpectrum()
                .getSignals())
                .allMatch(signalCompact -> signalCompact.getDoubles()[0] >= minShift
                        && signalCompact.getDoubles()[0] <= maxShift);
    }

    public static List<DataSet> filterByShift(final List<DataSet> dataSetList, final int minShift, final int maxShift) {
        return dataSetList.stream()
                .filter(dataSet -> checkShifts(dataSet, minShift, maxShift))
                .collect(Collectors.toList());
    }

    /**
     * Overwrite (non-)existing multiplicity value through protons count in signals
     * from given nucleus type
     *
     * @param dataSetList
     * @param nucleus
     */
    public static void setMultiplicityByProtonsCount(final List<DataSet> dataSetList, final String nucleus) {
        Spectrum spectrum;
        IAtomContainer structure;
        IAtom atom;
        for (final DataSet dataSet : dataSetList) {
            spectrum = dataSet.getSpectrum()
                    .toSpectrum();
            if (!spectrum.getNuclei()[0].equals(nucleus)) {
                continue;
            }
            structure = dataSet.getStructure()
                    .toAtomContainer();
            for (int i = 0; i < spectrum.getSignals()
                    .size(); i++) {
                atom = structure.getAtom(dataSet.getAssignment()
                        .getAssignment(0, i, 0));
                spectrum.getSignal(i)
                        .setMultiplicity(Utils.getMultiplicityFromProtonsCount(
                                AtomContainerManipulator.countHydrogens(structure,
                                        atom)));
            }
            dataSet.setSpectrum(new SpectrumCompact(spectrum));
        }
    }
}
