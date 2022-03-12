package org.openscience.sherlock.core.utils.elucidation;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Correlations;
import casekit.nmr.utils.Parser;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.sherlock.core.model.ElucidationOptions;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.Utilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Prediction {

    public static ResponseEntity<Transfer> parseAndPredictFromSmilesFile(final Correlations correlations,
                                                                         final ElucidationOptions elucidationOptions,
                                                                         final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap,
                                                                         final String pathToSmilesFile,
                                                                         final WebClient.Builder webClientBuilder,
                                                                         final ExchangeStrategies exchangeStrategies) {
        final Transfer responseTransfer = new Transfer();
        try {
            final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            final List<String> smilesList = Parser.smilesFileToList(pathToSmilesFile);
            //            System.out.println("-----> requestSMILES: "
            //                                       + smilesList.size());
            try {
                final List<IAtomContainer> structureList = new ArrayList<>();
                for (final String smiles : smilesList) {
                    structureList.add(smilesParser.parseSmiles(smiles));
                }
                final List<DataSet> dataSetList = predict(correlations, structureList, elucidationOptions,
                                                          hoseCodeDBEntriesMap, webClientBuilder, exchangeStrategies);
                responseTransfer.setDataSetList(dataSetList);
            } catch (final Exception e) {
                responseTransfer.setErrorMessage(e.getMessage());
                return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (final FileNotFoundException e) {
            //            System.out.println("--> could not parse SMILES file: "
            //                                       + requestTransfer.getPathToSmilesFile());
            responseTransfer.setDataSetList(new ArrayList<>());
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    public static List<DataSet> predict(final Correlations correlations, final List<IAtomContainer> structureList,
                                        final ElucidationOptions elucidationOptions,
                                        final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap,
                                        final WebClient.Builder webClientBuilder,
                                        final ExchangeStrategies exchangeStrategies) {
        // @TODO method modifications for different nuclei and solvent needed
        final String nucleus = "13C";
        final int maxSphere = 6;
        final int nThreads = 2;
        final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(correlations.getValues(), nucleus);

        return casekit.nmr.prediction.Prediction.predict1DByStereoHOSECodeAndFilter(querySpectrum,
                                                                                    elucidationOptions.getShiftTolerance(),
                                                                                    elucidationOptions.getMaximumAverageDeviation(),
                                                                                    maxSphere, structureList,
                                                                                    hoseCodeDBEntriesMap,
                                                                                    Objects.requireNonNull(
                                                                                            Utilities.getMultiplicitySectionsSettings(
                                                                                                             webClientBuilder,
                                                                                                             exchangeStrategies)
                                                                                                     .block()),
                                                                                    nThreads);
    }
}
