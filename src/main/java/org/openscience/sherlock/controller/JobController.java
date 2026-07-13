package org.openscience.sherlock.controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
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

@RestController
@RequestMapping(value = "/job")
public class JobController {

    @GetMapping(value = "/cancel")
    public ResponseEntity<Boolean> cancel(@RequestParam("id") String id) {
        try {
            GlobalJobScheduler.get().cancelJob(id);
            final boolean cancelled = GlobalJobScheduler.waitUntilCancelled(id, 2000, 500);
            return new ResponseEntity<>(cancelled, HttpStatus.OK);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/getJobStatus")
    public ResponseEntity<JobState> getJobStatus(@RequestParam("id") String id) {
        final JobState status = GlobalJobScheduler.get().getJobStatus(id);

        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @GetMapping(value = "/getJob")
    public ResponseEntity<JobSnapshot> getJob(@RequestParam("id") String id) {
        final JobSnapshot job = GlobalJobScheduler.get().getJobSnapshot(id);
        return new ResponseEntity<>(job, job == null ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }

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

    @GetMapping(value = "/getRunningJobs")
    public ResponseEntity<List<JobSnapshot>> getRunningJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getRunningJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @GetMapping(value = "/getQueuedJobs")
    public ResponseEntity<List<JobSnapshot>> getQueuedJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getQueuedJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @GetMapping(value = "/getFinishedJobs")
    public ResponseEntity<List<JobSnapshot>> getFinishedJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getFinishedJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @GetMapping(value = "/getCancelledJobs")
    public ResponseEntity<List<JobSnapshot>> getCancelledJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getCancelledJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @GetMapping(value = "/getErroredJobs")
    public ResponseEntity<List<JobSnapshot>> getErroredJobs() {
        final List<JobSnapshot> jobs = GlobalJobScheduler.get().getErroredJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    public ResponseEntity<RequestResult> getJobSnapshot(final RequestData requestData) {
        final RequestResult requestResult = Utilities
                .prepareDefaultRequestResult(requestData);
        requestResult.setRequestId(requestData.getRequestId());
        final ResponseEntity<JobSnapshot> jobSnapshotResponseEntity = this.getJob(requestData.getRequestId());
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
        final ResponseEntity<JobSnapshot> jobSnapshotResponseEntity = this.getJob(requestData.getRequestId());
        if (jobSnapshotResponseEntity == null || jobSnapshotResponseEntity.getBody() == null) {
            requestResult.setErrorMessage(
                    "No job found with ID: "
                            + requestData.getRequestId());
            requestResult.setIsCancelled(false);
            return new ResponseEntity<>(requestResult, HttpStatus.NOT_FOUND);
        }
        final ResponseEntity<Boolean> cancelationResponseEntity = this.cancel(requestData.getRequestId());
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
}
