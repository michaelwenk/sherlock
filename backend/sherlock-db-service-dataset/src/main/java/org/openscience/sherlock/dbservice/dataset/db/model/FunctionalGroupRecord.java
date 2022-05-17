package org.openscience.sherlock.dbservice.dataset.db.model;

import casekit.nmr.model.DataSet;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

@Document(collection = "functionalGroups")
public class FunctionalGroupRecord {

    @Id
    private String id;
    private DataSet dataSet;
}
