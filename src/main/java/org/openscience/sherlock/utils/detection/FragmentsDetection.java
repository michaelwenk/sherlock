package org.openscience.sherlock.utils.detection;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Correlation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.sherlock.model.exchange.Transfer;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.dbservice.dataset.controller.FragmentController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FragmentsDetection {

    @Autowired
    private FragmentController fragmentController;

    public List<DataSet> detect(
            final Spectrum querySpectrum,
            final List<Correlation> correlationsList, final String mf, final double shiftTol,
            final double maximumAverageDeviation, final int nThreads) {
        final List<List<Integer>> hybridizationList = new ArrayList<>();
        for (final Correlation correlation : correlationsList) {
            hybridizationList.add(correlation.getHybridization());
        }

        final Transfer queryTransfer = new Transfer();
        queryTransfer.setQuerySpectrum(querySpectrum);
        queryTransfer.setMf(mf);
        queryTransfer.setHybridizationList(hybridizationList);
        queryTransfer.setShiftTolerance(shiftTol);
        queryTransfer.setMaximumAverageDeviation(maximumAverageDeviation);
        queryTransfer.setNThreads(nThreads);

        final List<DataSet> fragmentList = fragmentController.getBySpectrumAndMfAndSetBits(queryTransfer).collectList()
                .block();
        if (fragmentList == null) {
            return new ArrayList<>();
        }

        return fragmentList.stream()
                .map(dataSet -> {
                    try {
                        Utilities.addMolFileToDataSet(dataSet);
                    } catch (final CDKException e) {
                        e.printStackTrace();
                    }
                    dataSet.addAttachment("include", false);

                    return dataSet;
                })
                .collect(Collectors.toList());
    }
}
