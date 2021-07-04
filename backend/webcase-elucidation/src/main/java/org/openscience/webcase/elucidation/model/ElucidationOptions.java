package org.openscience.webcase.elucidation.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions {
    // PyLSD options
    private boolean useFilterLsdRing3;
    private boolean useFilterLsdRing4;
    private boolean allowHeteroHeteroBonds;
    private boolean useElim;
    private int elimP1;
    private int elimP2;
    private int hmbcP3;
    private int hmbcP4;
    private int cosyP3;
    private int cosyP4;
    private float hybridizationDetectionThreshold;
    private int timeLimitTotal;
}
