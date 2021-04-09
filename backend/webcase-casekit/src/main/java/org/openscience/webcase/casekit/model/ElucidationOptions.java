package org.openscience.webcase.casekit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElucidationOptions {
    private String pathToResultsFile;
    private String pathToPyLSDExecutableFolder;
    private String pathToPyLSDInputFile;
    private String pathToPyLSDInputFileFolder;

    // PyLSD options
    private String[] filterPaths;
    private boolean allowHeteroHeteroBonds;
    private boolean useElim;
    private int elimP1;
    private int elimP2;
    private int hmbcP3;
    private int hmbcP4;
    private int cosyP3;
    private int cosyP4;
}
