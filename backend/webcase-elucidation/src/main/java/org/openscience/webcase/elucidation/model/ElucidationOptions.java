package org.openscience.webcase.elucidation.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions
        extends casekit.nmr.lsd.model.ElucidationOptions {
    // PyLSD input file creation
    private boolean useFilterLsdRing3;
    private boolean useFilterLsdRing4;
    private float hybridizationDetectionThreshold;
    // elucidation process
    private int timeLimitTotal;
    // generated structures filter
    private double maxAverageDeviation;
}
