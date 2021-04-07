package org.openscience.webcase.core.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions {
    private String pathToResultsFile;
    private String pathToPyLSDExecutableFolder;
    private String pathToPyLSDInputFileFolder;

    // PyLSD options
    private boolean useFilterLsdRing3;
    private boolean useFilterLsdRing4;
    private String[] filterPaths;
    private boolean allowHeteroHeteroBonds;
    private boolean useElim;
    private int elimP1;
    private int elimP2;
}
