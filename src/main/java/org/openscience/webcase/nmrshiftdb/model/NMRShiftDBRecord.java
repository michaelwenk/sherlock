package org.openscience.webcase.nmrshiftdb.model;

import lombok.*;
import org.openscience.webcase.nmr.model.bean.DataSetBean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

@Document(collection = "original")
public class NMRShiftDBRecord {

    @Id
    private String id;
    private String mf;
    private DataSetBean dataSetBean;
}
