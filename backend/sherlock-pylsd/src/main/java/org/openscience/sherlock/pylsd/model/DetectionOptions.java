package org.openscience.sherlock.pylsd.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DetectionOptions {

    private boolean useHybridizationDetections;
    private boolean useNeighborDetections;
    private float hybridizationDetectionThreshold;
    private float lowerElementCountThreshold;
    private float upperElementCountThreshold;
}
