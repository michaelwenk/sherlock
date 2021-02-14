package org.openscience.webcase.dbservice.dataset.model;

import lombok.Data;

import java.util.Map;

@Data
public class DataSet {

    private ExtendedConnectionMatrix structure;
    private Spectrum spectrum;
    private Assignment assignment;
    private Map<String, String> meta;
}
