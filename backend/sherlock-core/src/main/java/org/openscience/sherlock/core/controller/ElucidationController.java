package org.openscience.sherlock.core.controller;

import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.elucidation.PyLSD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

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
}
