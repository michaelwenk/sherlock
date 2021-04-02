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
    private String pathToLSDFilterList;
    private String pathToPyLSDInputFileFolder;

    // PyLSD options
    private boolean allowHeteroHeteroBonds;
}
