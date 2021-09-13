package org.openscience.webcase.pylsd.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions
        extends casekit.nmr.lsd.model.ElucidationOptions {
    // PyLSD options
    private float hybridizationDetectionThreshold;
    private float hybridizationCountThreshold;
    private float protonsCountThreshold;
    private boolean useFilterLsdRing3;
    private boolean useFilterLsdRing4;
    // elucidation process
    private int timeLimitTotal;
    // generated structures filter
    private double maxAverageDeviation;
}
