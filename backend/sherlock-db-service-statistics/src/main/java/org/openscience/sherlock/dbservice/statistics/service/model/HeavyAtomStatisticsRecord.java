package org.openscience.sherlock.dbservice.statistics.service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

@Document(collection = "heavyAtomStatistics")
public class HeavyAtomStatisticsRecord {

    @Id
    String id;
    private String elementsString;
    private String atomPair;
    private int count;
}
