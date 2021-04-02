package org.openscience.webcase.casekit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DereplicationOptions {
    private Map<String, Double> shiftTolerances;
    private boolean checkMultiplicity;
    private boolean checkEquivalencesCount;
}
