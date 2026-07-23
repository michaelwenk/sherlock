package org.openscience.sherlock.controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.job.model.JobRecord;
import org.openscience.sherlock.dbservice.job.repository.JobRecordRepository;
import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.RequestPasswordUtils;
import org.openscience.sherlock.utils.Utilities;
import org.openscience.sherlock.utils.elucidation.job.GlobalJobScheduler;
import org.openscience.sherlock.utils.elucidation.job.JobSnapshot;
import org.openscience.sherlock.utils.elucidation.job.JobState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Jobs", description = "Endpoints for cancelling and inspecting asynchronous Sherlock jobs.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/job")
public class JobController {

    private final JobRecordRepository jobRecordRepository;

    public JobController(final JobRecordRepository jobRecordRepository) {
        this.jobRecordRepository = jobRecordRepository;
    }

    @Operation(summary = "Cancel a job", description = "Requests cancellation of the job with the given ID and waits briefly for the scheduler to confirm the cancelled state.")
    @GetMapping(value = "/cancel")
    public ResponseEntity<Boolean> cancel(
            @Parameter(description = "Request ID returned when the asynchronous job was created.", example = "2d9c2d6f-6d4f-4d9f-94b9-13c9a4db9fd2", required = true) @RequestParam("id") String id,
            @Parameter(description = "Password that was returned when the asynchronous job was created.", example = "3fQ9xv0A7kLm2PzR", required = true) @RequestParam("requestPassword") String requestPassword) {
        if (!this.isAuthorized(id, requestPassword)) {
            return new ResponseEntity<>(false, HttpStatus.FORBIDDEN);
        }
        try {
            GlobalJobScheduler.get().cancelJob(id);
            final boolean cancelled = GlobalJobScheduler.waitUntilCancelled(id, 2000, 500);
            return new ResponseEntity<>(cancelled, HttpStatus.OK);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get job status", description = "Returns the current state of the job identified by the given ID.")
    @GetMapping(value = "/getJobStatus")
    public ResponseEntity<JobState> getJobStatus(
            @Parameter(description = "Request ID returned when the asynchronous job was created.", example = "2d9c2d6f-6d4f-4d9f-94b9-13c9a4db9fd2", required = true) @RequestParam("id") String id,
            @Parameter(description = "Password that was returned when the asynchronous job was created.", example = "3fQ9xv0A7kLm2PzR", required = true) @RequestParam("requestPassword") String requestPassword) {
        if (!this.isAuthorized(id, requestPassword)) {
            return new ResponseEntity<>(JobState.UNKNOWN, HttpStatus.FORBIDDEN);
        }
        final JobState status = GlobalJobScheduler.get().getJobStatus(id);

        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @Operation(summary = "Get a job snapshot", description = "Returns the scheduler snapshot for the given job ID, including status and any available metadata.")
    @GetMapping(value = "/getJob")
    public ResponseEntity<JobSnapshot> getJob(
            @Parameter(description = "Request ID returned when the asynchronous job was created.", example = "2d9c2d6f-6d4f-4d9f-94b9-13c9a4db9fd2", required = true) @RequestParam("id") String id,
            @Parameter(description = "Password that was returned when the asynchronous job was created.", example = "3fQ9xv0A7kLm2PzR", required = true) @RequestParam("requestPassword") String requestPassword) {
        if (!this.isAuthorized(id, requestPassword)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        final JobSnapshot job = GlobalJobScheduler.get().getJobSnapshot(id);
        return new ResponseEntity<>(job, job == null ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }

    @Operation(summary = "List jobs", description = "Returns all jobs or only the jobs matching the optional state filter.")
    @GetMapping(value = "/getJobs")
    public ResponseEntity<List<JobSnapshot>> getJobs(
            @RequestParam(value = "state", required = false) JobState state) {
        final List<JobSnapshot> jobs = state == null || state.toString().length() == 0
                ? Stream.concat(GlobalJobScheduler.get().getAllJobsInQueue().stream(),
                        GlobalJobScheduler.get().getAllTerminalJobs().stream())
                        .collect(Collectors.toList())
                : GlobalJobScheduler.get().getJobsByState(state);
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @Operation(summary = "List running jobs", description = "Returns all jobs that are currently running.")
    @GetMapping(value = "/getRunningJobs")
    public ResponseEntity<List<JobSnapshot>> getRunningJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getRunningJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @Operation(summary = "List queued jobs", description = "Returns all jobs that are waiting in the scheduler queue.")
    @GetMapping(value = "/getQueuedJobs")
    public ResponseEntity<List<JobSnapshot>> getQueuedJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getQueuedJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @Operation(summary = "List finished jobs", description = "Returns all jobs that completed successfully.")
    @GetMapping(value = "/getFinishedJobs")
    public ResponseEntity<List<JobSnapshot>> getFinishedJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getFinishedJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @Operation(summary = "List cancelled jobs", description = "Returns all jobs that were cancelled before completion.")
    @GetMapping(value = "/getCancelledJobs")
    public ResponseEntity<List<JobSnapshot>> getCancelledJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getCancelledJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @Operation(summary = "List errored jobs", description = "Returns all jobs that ended in an error state.")
    @GetMapping(value = "/getErroredJobs")
    public ResponseEntity<List<JobSnapshot>> getErroredJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getErroredJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    public ResponseEntity<RequestResult> getJobSnapshot(final RequestData requestData) {
        final RequestResult requestResult = Utilities
                .prepareDefaultRequestResult(requestData);
        requestResult.setRequestId(requestData.getRequestId());
        if (requestData.getRequestPassword() == null || requestData.getRequestPassword().isBlank()) {
            requestResult.setErrorMessage("Request password is missing for STATUS query type.");
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }
        if (!this.isAuthorized(requestData.getRequestId(), requestData.getRequestPassword())) {
            requestResult.setErrorMessage("Invalid request password for request ID: " + requestData.getRequestId());
            return new ResponseEntity<>(requestResult, HttpStatus.FORBIDDEN);
        }
        final JobSnapshot jobSnapshot = GlobalJobScheduler.get().getJobSnapshot(requestData.getRequestId());
        final ResponseEntity<JobSnapshot> jobSnapshotResponseEntity = new ResponseEntity<>(jobSnapshot,
                jobSnapshot == null ? HttpStatus.NOT_FOUND : HttpStatus.OK);
        if (jobSnapshotResponseEntity == null || jobSnapshotResponseEntity.getBody() == null) {
            requestResult.setErrorMessage(
                    "No job found with ID: "
                            + requestData.getRequestId());
            return new ResponseEntity<>(requestResult, HttpStatus.NOT_FOUND);
        } else {
            requestResult.setJobState(jobSnapshotResponseEntity.getBody());
            return new ResponseEntity<>(requestResult, HttpStatus.OK);
        }
    }

    public ResponseEntity<RequestResult> cancelJob(final RequestData requestData) {
        final RequestResult requestResult = Utilities
                .prepareDefaultRequestResult(requestData);
        if (requestData.getRequestPassword() == null || requestData.getRequestPassword().isBlank()) {
            requestResult.setErrorMessage("Request password is missing for CANCEL query type.");
            requestResult.setIsCancelled(false);
            return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
        }
        if (!this.isAuthorized(requestData.getRequestId(), requestData.getRequestPassword())) {
            requestResult.setErrorMessage("Invalid request password for request ID: " + requestData.getRequestId());
            requestResult.setIsCancelled(false);
            return new ResponseEntity<>(requestResult, HttpStatus.FORBIDDEN);
        }
        final JobSnapshot jobSnapshot = GlobalJobScheduler.get().getJobSnapshot(requestData.getRequestId());
        final ResponseEntity<JobSnapshot> jobSnapshotResponseEntity = new ResponseEntity<>(jobSnapshot,
                jobSnapshot == null ? HttpStatus.NOT_FOUND : HttpStatus.OK);
        if (jobSnapshotResponseEntity == null || jobSnapshotResponseEntity.getBody() == null) {
            requestResult.setErrorMessage(
                    "No job found with ID: "
                            + requestData.getRequestId());
            requestResult.setIsCancelled(false);
            return new ResponseEntity<>(requestResult, HttpStatus.NOT_FOUND);
        }
        final ResponseEntity<Boolean> cancelationResponseEntity = this.cancel(requestData.getRequestId(),
                requestData.getRequestPassword());
        if (cancelationResponseEntity.getBody() != null
                && cancelationResponseEntity.getBody() == true) {
            requestResult.setIsCancelled(true);
            requestResult
                    .setJobState(new JobSnapshot(requestData.getRequestId(), JobState.CANCELLED, null, null));
        } else {
            requestResult.setIsCancelled(false);
            requestResult.setErrorMessage(
                    "Failed to cancel job with ID: "
                            + requestData.getRequestId());
            requestResult
                    .setJobState(new JobSnapshot(requestData.getRequestId(), JobState.UNKNOWN, null, null));
        }
        return new ResponseEntity<>(requestResult, HttpStatus.OK);
    }

    private boolean isAuthorized(final String jobId, final String requestPassword) {
        if (jobId == null || jobId.isBlank() || requestPassword == null || requestPassword.isBlank()) {
            return false;
        }

        final JobRecord jobRecord = this.jobRecordRepository.findByJobId(jobId);
        if (jobRecord == null || jobRecord.getRequestPasswordHash() == null
                || jobRecord.getRequestPasswordHash().isBlank()) {
            return false;
        }

        return RequestPasswordUtils.matches(requestPassword, jobRecord.getRequestPasswordHash());
    }
}
