package org.openscience.sherlock.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.openscience.sherlock.dbservice.result.model.ResultRecord;
import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.Utilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping(value = "/retrieval")
public class RetrievalController {

    private final ResultController resultController;

    public RetrievalController(final ResultController resultController) {
        this.resultController = resultController;
    }

    @GetMapping("/getByRequestId")
    public ResponseEntity<RequestResult> getByRequestId(@RequestBody final RequestData requestData) {
        final RequestResult requestResult = Utilities
                .prepareDefaultRequestResult(requestData);
        if (requestData.getRequestId() == null
                || requestData.getRequestId().isEmpty()) {
            requestResult.setErrorMessage(
                    "Request ID is missing for RETRIEVE query type.");
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }
        final ResultRecord resultRecord = this.resultController
                .getByRequestId(requestData.getRequestId()).block();
        requestResult.setResultRecord(resultRecord);

        return new ResponseEntity<>(requestResult, HttpStatus.OK);
    }

}
