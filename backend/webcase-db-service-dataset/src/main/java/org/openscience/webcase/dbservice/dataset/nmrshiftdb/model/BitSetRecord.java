package org.openscience.webcase.dbservice.dataset.nmrshiftdb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@AllArgsConstructor
@Data

@Document(collection = "bitsets")
public class BitSetRecord {

    @Id
    private String id;
    private String nucleus;
    private long fingerprintSize;
    private int[] setBits;
    private int setBitsCount;
    private List<String> dataSetRecordIDs;
}
