package org.openscience.sherlock.dbservice.result.model;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PredictionOptions {

    private List<String> smilesList;
    private int maxSphere;
    private boolean checkMultiplicity;
    private boolean checkEquivalencesCount;
    private boolean allowLowerEquivalencesCount;
    private MultiplicitySectionsBuilder multiplicitySectionsBuilder;
    private double shiftTolerance;
    private double maximumAverageDeviation;
    private boolean predictWithStereo;
}
