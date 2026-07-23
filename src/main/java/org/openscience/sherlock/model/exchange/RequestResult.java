package org.openscience.sherlock.model.exchange;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openscience.sherlock.dbservice.result.model.ResultRecord;
import org.openscience.sherlock.model.DereplicationOptions;
import org.openscience.sherlock.utils.elucidation.job.JobSnapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Schema(name = "RequestResult", description = "Response payload returned by /query endpoint.", example = "{\"requestId\":\"2d9c2d6f-6d4f-4d9f-94b9-13c9a4db9fd2\",\"errorMessage\":null,\"isCancelled\":false}")
public class RequestResult {
    // request id
    @Schema(description = "Request identifier used to track async work.", example = "2d9c2d6f-6d4f-4d9f-94b9-13c9a4db9fd2")
    private String requestId;

    // request password
    @Schema(description = "Password generated for an asynchronous job and required for later retrieval, status, or cancellation.", example = "3fQ9xv0A7kLm2PzR")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String requestPassword;

    // error message
    @Schema(description = "Error details if processing failed.", example = "Invalid query type: UNKNOWN")
    private String errorMessage;

    // dereplication options
    @Schema(description = "Dereplication options reflected in the response where applicable.")
    private DereplicationOptions dereplicationOptions;

    // result record
    @Schema(description = "Resolved result record for retrieval-related responses.")
    private ResultRecord resultRecord;

    // job state as snapshot
    @Schema(description = "Current job state for STATUS requests.")
    private JobSnapshot jobState;

    // job cancelation flag
    @Schema(description = "Cancellation flag for CANCEL requests.", example = "false")
    private Boolean isCancelled;
}
