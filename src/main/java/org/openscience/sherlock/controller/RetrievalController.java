package org.openscience.sherlock.controller;

import java.io.IOException;

import org.openscience.cdk.exception.CDKException;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.openscience.sherlock.dbservice.result.model.ResultRecord;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.RequestPasswordUtils;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.utils.elucidation.job.GlobalJobScheduler;
import org.openscience.sherlock.utils.elucidation.job.JobSnapshot;
import org.openscience.sherlock.utils.elucidation.job.JobState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Retrieval", description = "Endpoints for retrieving persisted Sherlock results.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/retrieval")
public class RetrievalController {

    @Value("${sherlock.version}")
    private String sherlockVersion;

    private final ResultController resultController;

    public RetrievalController(final ResultController resultController) {
        this.resultController = resultController;
    }

    @Operation(summary = "Retrieve a result by request ID", description = "Loads the stored Sherlock result for the given request ID.")
    @GetMapping("/getByRequestId")
    public ResponseEntity<RequestResult> getByRequestId(
            @Parameter(description = "Request ID returned when the asynchronous job was created.", example = "3fQ9xv0A7kLm2PzR", required = true) @RequestParam final String id,
            @Parameter(description = "Password that was returned when the asynchronous job was created.", example = "3fQ9xv0A7kLm2PzR", required = true) @RequestParam final String requestPassword) {
        final RequestResult requestResult = new RequestResult();
        requestResult.setRequestId(id);
        if (id == null
                || id.isEmpty()) {
            requestResult.setErrorMessage(
                    "Request ID is missing for RETRIEVE query type.");
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }
        if (requestPassword == null || requestPassword.isEmpty()) {
            requestResult.setErrorMessage(
                    "Request password is missing for RETRIEVE query type.");
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }
        final ResultRecord resultRecord = this.resultController
                .findByRequestId(id).block();
        if (resultRecord == null) {
            requestResult.setErrorMessage("No result found for request ID: " + id);
            return new ResponseEntity<>(requestResult, HttpStatus.NOT_FOUND);
        }
        if (!RequestPasswordUtils.matches(requestPassword, resultRecord.getRequestPasswordHash())) {
            requestResult.setErrorMessage("Invalid request password for request ID: " + id);
            return new ResponseEntity<>(requestResult, HttpStatus.FORBIDDEN);
        }
        requestResult.setResultRecord(resultRecord);
        final JobSnapshot jobSnapshot = GlobalJobScheduler.get().getJobSnapshot(id);
        requestResult.setJobState(jobSnapshot != null
                ? jobSnapshot
                : new JobSnapshot(id, JobState.UNKNOWN, null, null));

        return new ResponseEntity<>(requestResult, HttpStatus.OK);
    }

    @Operation(summary = "Retrieve a result in SD file format by request ID", description = "Loads the stored Sherlock result for the given request ID.")
    @GetMapping("/getSdfByRequestId")
    public ResponseEntity<String> getSdfByRequestId(
            @Parameter(description = "Request ID returned when the asynchronous job was created.", example = "3fQ9xv0A7kLm2PzR", required = true) @RequestParam final String id,
            @Parameter(description = "Password that was returned when the asynchronous job was created.", example = "3fQ9xv0A7kLm2PzR", required = true) @RequestParam final String requestPassword) {

        if (id == null
                || id.isEmpty()) {
            return new ResponseEntity<>("Request ID is missing for RETRIEVE query type.", HttpStatus.BAD_REQUEST);
        }
        if (requestPassword == null || requestPassword.isEmpty()) {
            return new ResponseEntity<>("Request password is missing for RETRIEVE query type.", HttpStatus.BAD_REQUEST);
        }
        final ResultRecord resultRecord = this.resultController
                .findByRequestId(id).block();
        if (resultRecord == null) {
            return new ResponseEntity<>("No result found for request ID: " + id, HttpStatus.NOT_FOUND);
        }
        if (!RequestPasswordUtils.matches(requestPassword, resultRecord.getRequestPasswordHash())) {
            return new ResponseEntity<>("Invalid request password for request ID: " + id, HttpStatus.FORBIDDEN);
        }
        String sdf = null;
        try {
            sdf = Utilities.convertResultRecordToSdfString(resultRecord, sherlockVersion);
        } catch (IOException | CDKException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error converting result to SD format for request ID: " + id,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (sdf == null || sdf.isEmpty()) {
            return new ResponseEntity<>("No SD format result available for request ID: " + id, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(sdf, HttpStatus.OK);
    }

}
