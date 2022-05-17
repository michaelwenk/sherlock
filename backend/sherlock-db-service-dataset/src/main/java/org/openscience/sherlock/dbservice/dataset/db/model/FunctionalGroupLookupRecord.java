package org.openscience.sherlock.dbservice.dataset.db.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString

@Document(collection = "functionalGroupLookup")
public class FunctionalGroupLookupRecord {

    @Id
    private String id;
    private int setBit;
    private Set<String> ids;
}
