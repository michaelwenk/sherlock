package org.openscience.webcase.dbservice.hosecode.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions
        extends casekit.nmr.lsd.model.ElucidationOptions {
    // generated structures filter
    private double maxAverageDeviation;
}
