package org.openscience.sherlock.model.exchange;

import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.nmrium.Correlations;
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
public class RequestData {

    /**
     * The type of query to be performed (see
     * {@link org.openscience.sherlock.model.QueryTypes}).
     */
    private String queryType;
    /**
     * The name of the query, which can later be used for retrieval purposes.
     */
    private String name;
    /**
     * The unique identifier of the query, which is used for cancellation purposes.
     */
    private String requestId;
    /**
     * The correlation data associated with the query.
     * See {@link casekit.nmr.model.nmrium.Correlations} for details on the
     * available correlation data.
     */
    private Correlations correlations;
    /**
     * The dereplication options associated with the query.
     * See {@link org.openscience.sherlock.model.DereplicationOptions} for details
     * on the available options.
     */
    private DereplicationOptions dereplicationOptions;
    /**
     * Indicates whether detection was already performed.
     */
    private Boolean detected;
    /**
     * The detections associated with the query.
     * See {@link casekit.nmr.elucidation.model.Detections} for details on the
     * available detections.
     */
    private Detections detections;
    /**
     * The detection options associated with the query.
     * See {@link org.openscience.sherlock.model.DetectionOptions} for details on
     * the available options.
     */
    private DetectionOptions detectionOptions;
    /**
     * The elucidation options associated with the query.
     * See {@link org.openscience.sherlock.model.ElucidationOptions} for details on
     * the available options.
     */
    private ElucidationOptions elucidationOptions;
    /**
     * The grouping associated with the query.
     * See {@link casekit.nmr.elucidation.model.Grouping} for details on the
     * available grouping.
     */
    private Grouping grouping;
}
