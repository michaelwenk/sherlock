package org.openscience.webcase.dbservice.result.model;

import lombok.*;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Result {

    private String smiles;
    private Map<String, Double> metrics;
}
