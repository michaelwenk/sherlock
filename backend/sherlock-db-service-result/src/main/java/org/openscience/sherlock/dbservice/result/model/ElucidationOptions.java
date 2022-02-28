package org.openscience.sherlock.dbservice.result.model;

import lombok.Data;

@Data
public class ElucidationOptions
        extends casekit.nmr.elucidation.model.ElucidationOptions {
    // PyLSD input file creation
    private boolean useFilterLsdRing3;
    private boolean useFilterLsdRing4;
    // elucidation process
    private int timeLimitTotal;
    private boolean useCombinatorics;
}
