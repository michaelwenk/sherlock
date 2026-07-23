package org.openscience.sherlock.controller;

import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.openscience.sherlock.dbservice.result.model.ResultRecord;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.RequestPasswordUtils;
import org.openscience.sherlock.utils.elucidation.job.GlobalJobScheduler;
import org.openscience.sherlock.utils.elucidation.job.JobSnapshot;
import org.openscience.sherlock.utils.elucidation.job.JobState;
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

    private final ResultController resultController;

    public RetrievalController(final ResultController resultController) {
        this.resultController = resultController;
    }

    @Operation(summary = "Retrieve a result by request ID", description = "Loads the stored Sherlock result for the given request ID.")
    @GetMapping("/getByRequestId")
    public ResponseEntity<RequestResult> getByRequestId(
            @Parameter(description = "Request ID returned when the asynchronous job was created.", example = "2d9c2d6f-6d4f-4d9f-94b9-13c9a4db9fd2", required = true) @RequestParam final String id,
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

}
