package org.openscience.sherlock.core.controller;

import casekit.nmr.model.DataSet;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.elucidation.Prediction;
import org.openscience.sherlock.core.utils.elucidation.PyLSD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/elucidation")
public class ElucidationController {


    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap;

    @Autowired
    public ElucidationController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies,
                                 final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.hoseCodeDBEntriesMap = hoseCodeDBEntriesMap;
    }

    @PostMapping(value = "/elucidate")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        // run PyLSD
        try {
            final ResponseEntity<Transfer> responseEntity = PyLSD.runPyLSD(requestTransfer, this.hoseCodeDBEntriesMap,
                                                                           this.webClientBuilder,
                                                                           this.exchangeStrategies);
            if (responseEntity.getStatusCode()
                              .isError()) {
                responseTransfer.setErrorMessage(responseEntity.getBody()
                                                         != null
                                                 ? responseEntity.getBody()
                                                                 .getErrorMessage()
                                                 : "Something went wrong when trying to run PyLSD!!!");
                return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
            }
            final Transfer queryResultTransfer = responseEntity.getBody();
            responseTransfer.setPyLSDRunWasSuccessful(queryResultTransfer.getPyLSDRunWasSuccessful());
            responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());
            responseTransfer.setDetections(queryResultTransfer.getDetections());
            responseTransfer.setGrouping(queryResultTransfer.getGrouping());
            responseTransfer.setDetectionOptions(queryResultTransfer.getDetectionOptions());
        } catch (final Exception e) {
            responseTransfer.setErrorMessage(e.getMessage());
            return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    @PostMapping(value = "/predict")
    public ResponseEntity<Transfer> predict(@RequestBody final Transfer requestTransfer) {
        List<DataSet> dataSetList = requestTransfer.getDataSetList()
                                            != null
                                    ? requestTransfer.getDataSetList()
                                    : new ArrayList<>();
        final List<IAtomContainer> structureList = new ArrayList<>();
        for (final DataSet dataSet : dataSetList) {
            structureList.add(dataSet.getStructure()
                                     .toAtomContainer());
        }
        dataSetList = Prediction.predictAndFilter(requestTransfer.getCorrelations(), structureList,
                                                  requestTransfer.getElucidationOptions(), this.hoseCodeDBEntriesMap,
                                                  this.webClientBuilder, this.exchangeStrategies);
        requestTransfer.setDataSetList(dataSetList);

        return new ResponseEntity<>(requestTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "/predictBySmiles")
    public DataSet predict(@RequestParam final String smiles, final String nucleus, final Integer maxSphere) {
        try {
            final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            final IAtomContainer structure = smilesParser.parseSmiles(smiles);

            return Prediction.predict(structure, nucleus
                                                         != null
                                                 ? nucleus
                                                 : "13C", maxSphere
                                                                  != null
                                                          ? maxSphere
                                                          : 6, this.hoseCodeDBEntriesMap);
        } catch (final InvalidSmilesException e) {
            e.printStackTrace();
        }

        return null;
    }
}
