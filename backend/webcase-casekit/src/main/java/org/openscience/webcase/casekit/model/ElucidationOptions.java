package org.openscience.webcase.casekit.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions {
    private String pathToResultsFile;
    private String pathToPyLSDExecutableFolder;
    private String pathToLSDFilterList;
    private String pathToPyLSDInputFile;
    private String pathToPyLSDInputFileFolder;

    private String mf;
    private boolean allowHeteroHeteroBonds;
}
