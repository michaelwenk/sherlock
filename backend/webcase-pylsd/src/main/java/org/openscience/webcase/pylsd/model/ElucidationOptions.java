package org.openscience.webcase.pylsd.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions {
    // PyLSD options
    private String[] filterPaths;
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
}
