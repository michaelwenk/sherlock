package org.openscience.sherlock.controller;

import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.IdGenerator;
import org.openscience.sherlock.utils.RequestPasswordUtils;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.utils.elucidation.PyLSD;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Elucidation", description = "Endpoints for running synchronous and asynchronous PyLSD elucidation jobs.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/elucidation")
public class ElucidationController {

    private final PyLSD pyLSD;

    public ElucidationController(final PyLSD pyLSD, ResultController resultController) {
        this.pyLSD = pyLSD;
    }

    @Operation(summary = "Run synchronous elucidation", description = "Executes the PyLSD elucidation workflow immediately and returns the completed result in the response.")
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

    @Operation(summary = "Schedule asynchronous elucidation", description = "Queues a PyLSD elucidation job, returns the generated request ID, and lets the client poll job and result endpoints later.")
    @PostMapping(value = "/elucidateAsync")
    public ResponseEntity<RequestResult> elucidateAsync(@RequestBody final RequestData requestData) {
        // INPUT DATA CHECK
        final RequestResult requestResult = Utilities.prepareDefaultRequestResult(requestData);
        if (requestResult.getErrorMessage() != null) {
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }

        // NEW INTERNAL ID CREATION
        final String requestId = IdGenerator.generateId();
        final String requestPassword = RequestPasswordUtils.generatePassword();
        requestResult.setRequestId(requestId);
        requestResult.setRequestPassword(requestPassword);

        pyLSD.schedulePyLSD(
                requestId,
                RequestPasswordUtils.hashPassword(requestPassword),
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
