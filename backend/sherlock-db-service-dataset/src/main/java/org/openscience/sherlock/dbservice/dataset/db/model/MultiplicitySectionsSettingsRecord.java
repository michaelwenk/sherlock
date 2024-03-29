package org.openscience.sherlock.dbservice.dataset.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@Data

@Document(collection = "multiplicitySectionsSettings")
public class MultiplicitySectionsSettingsRecord {

    @Id
    final String id;
    final String nucleus;
    private int[] multiplicitySectionsSettings; // minLimit, maxLimit, stepSize
}
