package org.openscience.sherlock.core.model.db;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class HeavyAtomStatisticsRecord {

    String id;
    private String elementsString;
    private String atomPair;
    private int count;
}
