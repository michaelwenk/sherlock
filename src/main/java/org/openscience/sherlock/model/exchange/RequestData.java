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
    private String queryType;
    private String name;
    private String requestId;
    private Correlations correlations;
    private DereplicationOptions dereplicationOptions;
    private Boolean detected;
    private Detections detections;
    private DetectionOptions detectionOptions;
    private ElucidationOptions elucidationOptions;
    private Grouping grouping;
}
