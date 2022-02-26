package org.openscience.sherlock.core.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DereplicationOptions {
    private double shiftTolerance;
    private double maximumAverageDeviation;
    private boolean checkMultiplicity;
    private boolean checkEquivalencesCount;
    private boolean useMF;
}
