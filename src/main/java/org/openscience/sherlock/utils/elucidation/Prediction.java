package org.openscience.sherlock.utils.elucidation;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Correlations;
import casekit.nmr.utils.Parser;
import casekit.nmr.utils.Utils;
import org.openscience.sherlock.model.ElucidationOptions;
import org.openscience.sherlock.model.exchange.Transfer;
import org.openscience.sherlock.dbservice.dataset.controller.DataSetController;
import org.openscience.sherlock.dbservice.statistics.controller.HOSECodeController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

@Component
public class Prediction {

        @Autowired
        private DataSetController dataSetController;

        @Autowired
        private HOSECodeController hoseCodeController;

        public List<DataSet> parseAndPredictFromSmilesFile(final Correlations correlations,
                        final ElucidationOptions elucidationOptions,
                        final Detections detections,
                        final String pathToSmilesFile) {
                try {
                        final List<String> smilesList = Parser.smilesFileToList(pathToSmilesFile);
                        try {
                                final List<DataSet> dataSetList = predictAndFilter(correlations, smilesList,
                                                elucidationOptions,
                                                detections);
                                return dataSetList;
                        } catch (final Exception e) {
                                System.out.println("--> prediction error: "
                                                + e.getMessage());
                                return null;
                        }
                } catch (final FileNotFoundException e) {
                        System.out.println("--> could not parse SMILES file: "
                                        + pathToSmilesFile + " -> " + e.getMessage());
                }

                return null;
        }

        public List<DataSet> predictAndFilter(final Correlations correlations,
                        final List<String> smilesList,
                        final ElucidationOptions elucidationOptions,
                        final Detections detections) {
                // @TODO method modifications for different nuclei and solvent needed
                final String nucleus = "13C";
                final int maxSphere = 6;
                final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(correlations.getValues(), nucleus);
                final Map<String, int[]> multiplicitySectionsSettings = dataSetController
                                .getMultiplicitySectionsSettings();
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setQuerySpectrum(querySpectrum);
                queryTransfer.setShiftTolerance(elucidationOptions.getShiftTolerance());
                queryTransfer.setMaximumAverageDeviation(elucidationOptions.getMaximumAverageDeviation());
                queryTransfer.setCheckMultiplicity(true);
                queryTransfer.setCheckEquivalencesCount(true);
                queryTransfer.setAllowLowerEquivalencesCount(false);
                final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
                multiplicitySectionsBuilder
                                .setMinLimit(multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[0]);
                multiplicitySectionsBuilder
                                .setMaxLimit(multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[1]);
                multiplicitySectionsBuilder
                                .setStepSize(multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[2]);
                queryTransfer.setMultiplicitySectionsBuilder(multiplicitySectionsBuilder);
                queryTransfer.setDetections(detections);
                queryTransfer.setSmilesList(smilesList);
                queryTransfer.setMaxSphere(maxSphere);

                return hoseCodeController.predictAndFilter(queryTransfer)
                                .collectList()
                                .block();

        }

}
