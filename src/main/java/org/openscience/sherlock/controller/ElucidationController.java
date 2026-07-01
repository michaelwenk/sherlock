package org.openscience.sherlock.controller;

import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.IdGenerator;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.utils.elucidation.PyLSD;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/elucidation")
public class ElucidationController {

    private final PyLSD pyLSD;

    public ElucidationController(final PyLSD pyLSD, ResultController resultController) {
        this.pyLSD = pyLSD;
    }

    @PostMapping(value = "/elucidate")
    public ResponseEntity<RequestResult> elucidate(@RequestBody final RequestData requestData) {
        // INPUT DATA CHECK
        final RequestResult requestResult = Utilities.prepareDefaultRequestResult(requestData);
        if (requestResult.getErrorMessage() != null) {
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }

        // EXECUTE PYLSD
        try {
            final String requestId = IdGenerator.generateId() + "_temp";
            final ResponseEntity<RequestResult> response = pyLSD.runPyLSD(requestId,
                    requestResult.getResultRecord().getName(),
                    requestResult.getResultRecord().getCorrelations(),
                    requestResult.getResultRecord().getQuerySpectrum(),
                    requestResult.getResultRecord().getDetected(),
                    requestResult.getResultRecord().getDetectionOptions(),
                    requestResult.getResultRecord().getDetections(),
                    requestResult.getResultRecord().getGrouping(),
                    requestResult.getResultRecord().getElucidationOptions());

            if (response.getStatusCode() != HttpStatus.OK) {
                requestResult.setErrorMessage("PyLSD execution failed with status: " + response.getStatusCode());
                return new ResponseEntity<>(requestResult, response.getStatusCode());
            }

            final RequestResult responseBody = response.getBody();
            if (responseBody == null) {
                requestResult.setErrorMessage("PyLSD execution returned null response body.");
                return new ResponseEntity<>(requestResult, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            requestResult.setRequestId(requestId);
            requestResult.setResultRecord(responseBody.getResultRecord());

            return new ResponseEntity<>(requestResult, HttpStatus.OK);
        } catch (final Exception e) {
            System.err.println("An error occurred: ");
            e.printStackTrace();

            requestResult.setErrorMessage(e.getMessage());
            requestResult.getResultRecord()
                    .setQuerySpectrum(requestResult.getResultRecord().getQuerySpectrum());
            return new ResponseEntity<>(requestResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/elucidateAsync")
    public ResponseEntity<RequestResult> elucidateAsync(@RequestBody final RequestData requestData) {
        // INPUT DATA CHECK
        final RequestResult requestResult = Utilities.prepareDefaultRequestResult(requestData);
        if (requestResult.getErrorMessage() != null) {
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }

        // NEW INTERNAL ID CREATION
        final String requestId = IdGenerator.generateId();
        requestResult.setRequestId(requestId);

        System.out.println("Scheduling PyLSD job with request ID: "
                + requestId
                + " and request data: \n"
                + requestData
                + "\n");

        pyLSD.schedulePyLSD(
                requestId,
                requestResult.getResultRecord().getName(),
                requestResult.getResultRecord().getCorrelations(),
                requestResult.getResultRecord().getQuerySpectrum(),
                requestResult.getResultRecord().getDetected(),
                requestResult.getResultRecord().getDetectionOptions(),
                requestResult.getResultRecord().getDetections(),
                requestResult.getResultRecord().getGrouping(),
                requestResult.getResultRecord().getElucidationOptions());

        return new ResponseEntity<>(requestResult, HttpStatus.ACCEPTED);
    }

}
