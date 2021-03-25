package org.openscience.webcase.pylsd.run.model;

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

    private boolean allowHeteroHeteroBonds;
}
