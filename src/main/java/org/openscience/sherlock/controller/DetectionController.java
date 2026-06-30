package org.openscience.sherlock.controller;

import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.model.exchange.Transfer;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.utils.detection.Detection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping(value = "/detection")
public class DetectionController {

    private final Detection detection;

    public DetectionController(Detection detection) {
        this.detection = detection;
    }

    @PostMapping(value = "/detect")
    public ResponseEntity<RequestResult> detect(@RequestBody final RequestData requestData) {

        final RequestResult requestResult = Utilities.prepareDefaultRequestResult(requestData);
        if (requestResult.getErrorMessage() != null) {
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }

        final String mf = Utilities
                .getMolecularFormulaFromCorrelations(requestData.getCorrelations());

        final Transfer queryTransfer = new Transfer();
        queryTransfer.setQuerySpectrum(
                requestResult.getResultRecord().getQuerySpectrum().toSpectrum());
        queryTransfer.setCorrelations(requestData.getCorrelations());
        queryTransfer.setDetectionOptions(requestData.getDetectionOptions());
        queryTransfer.setMf(mf);
        queryTransfer.setDetections(requestData.getDetections());
        queryTransfer.setElucidationOptions(requestData.getElucidationOptions());

        final Transfer queryResultTransfer = detection.detect(queryTransfer);

        requestResult.getResultRecord()
                .setDetected(queryResultTransfer.getDetected());
        requestResult.getResultRecord()
                .setDetections(queryResultTransfer.getDetections());
        requestResult.getResultRecord()
                .setGrouping(queryResultTransfer.getGrouping());
        requestResult.getResultRecord()
                .setElucidationOptions(queryResultTransfer.getElucidationOptions());

        return new ResponseEntity<>(requestResult, HttpStatus.OK);
    }

}
