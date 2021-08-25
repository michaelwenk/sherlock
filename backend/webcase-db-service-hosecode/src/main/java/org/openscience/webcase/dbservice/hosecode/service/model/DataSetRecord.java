package org.openscience.webcase.dbservice.hosecode.service.model;

import casekit.nmr.model.DataSet;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

public class DataSetRecord {

    private String id;
    private DataSet dataSet;
}
