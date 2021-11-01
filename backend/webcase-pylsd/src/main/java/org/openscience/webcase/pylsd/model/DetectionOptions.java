package org.openscience.webcase.pylsd.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DetectionOptions {

    private float hybridizationDetectionThreshold;
    private float elementCountThreshold;
}
