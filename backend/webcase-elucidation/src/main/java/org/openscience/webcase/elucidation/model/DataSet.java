package org.openscience.webcase.elucidation.model;


import lombok.*;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DataSet {

    private ExtendedConnectionMatrix structure;
    private Spectrum spectrum;
    private Assignment assignment;
    private Map<String, String> meta;
}
