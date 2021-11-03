package org.openscience.webcase.core.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DetectionOptions {

    private float hybridizationDetectionThreshold;
    private float lowerElementCountThreshold;
    private float upperElementCountThreshold;
}
