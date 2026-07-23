package org.openscience.sherlock.model.exchange;

import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.nmrium.Correlations;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openscience.sherlock.model.DereplicationOptions;
import org.openscience.sherlock.model.DetectionOptions;
import org.openscience.sherlock.model.ElucidationOptions;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Schema(name = "RequestData", description = "Request payload for /query endpoint.", example = "{\"name\":\"demo-dereplication\",\"detected\":false,\"requestId\":null}")
public class RequestData {

    /**
     * The name of the query, which can later be used for retrieval purposes.
     */
    @Schema(description = "Optional user-defined name for the request.", example = "demo-dereplication")
    private String name;
    /**
     * The unique identifier of the query, which is used for cancellation purposes.
     */
    @Schema(description = "Request identifier for retrieval, status, or cancellation.", example = "2d9c2d6f-6d4f-4d9f-94b9-13c9a4db9fd2")
    private String requestId;

    @Schema(description = "Password required to retrieve, inspect, or cancel an asynchronous job.", example = "3fQ9xv0A7kLm2PzR")
    private String requestPassword;

    /**
     * The correlation data associated with the query.
     * See {@link casekit.nmr.model.nmrium.Correlations} for details on the
     * available correlation data.
     */
    @Schema(description = "NMR correlation payload used by detection/dereplication/elucidation workflows.")
    private Correlations correlations;
    /**
     * The dereplication options associated with the query.
     * See {@link org.openscience.sherlock.model.DereplicationOptions} for details
     * on the available options.
     */
    @Schema(description = "Dereplication options for a DEREPLICATION query.")
    private DereplicationOptions dereplicationOptions;
    /**
     * Indicates whether detection was already performed.
     */
    @Schema(description = "Indicates whether detection was already performed.", example = "false")
    private Boolean detected;
    /**
     * The detections associated with the query.
     * See {@link casekit.nmr.elucidation.model.Detections} for details on the
     * available detections.
     */
    @Schema(description = "Detection payload, if available.")
    private Detections detections;
    /**
     * The detection options associated with the query.
     * See {@link org.openscience.sherlock.model.DetectionOptions} for details on
     * the available options.
     */
    @Schema(description = "Detection options for a DETECTION query.")
    private DetectionOptions detectionOptions;
    /**
     * The elucidation options associated with the query.
     * See {@link org.openscience.sherlock.model.ElucidationOptions} for details on
     * the available options.
     */
    @Schema(description = "Elucidation options for ELUCIDATION and ELUCIDATION_ASYNC queries.")
    private ElucidationOptions elucidationOptions;
    /**
     * The grouping associated with the query.
     * See {@link casekit.nmr.elucidation.model.Grouping} for details on the
     * available grouping.
     */
    @Schema(description = "Grouping constraints for elucidation tasks.")
    private Grouping grouping;
}
