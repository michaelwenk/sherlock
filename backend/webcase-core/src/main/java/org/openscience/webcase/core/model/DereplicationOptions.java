package org.openscience.webcase.core.model;

import lombok.*;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DereplicationOptions {
    private Map<String, Double> shiftTolerances;
    private boolean checkMultiplicity;
    private boolean checkEquivalencesCount;
    private boolean useMF;
}
