package org.openscience.sherlock.dbservice.statistics.service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

@Document(collection = "connectivities")
public class ConnectivityRecord {

    @Id
    String id;
    private String nucleus;
    private String hybridization;
    private String multiplicity;
    private int shift;
    private Map<String, Map<String, Map<Integer, Integer>>> connectivityCounts; // connected atom symbol -> connected atom hybridization -> connected atom protons count -> occurrence
    private Map<String, Map<String, Integer[]>> occurrenceCounts; // "elemental composition" -> connected atom symbol -> [#found, #notFound]
}
