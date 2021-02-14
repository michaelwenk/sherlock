package org.openscience.webcase.ranking.spectralsimilarity.model;


import lombok.*;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DataSet {

    private ExtendedConnectionMatrix structure;
    private Spectrum spectrum;
    private Assignment assignment;
    private Map<String, Object> meta;

    public void addMetaInfo(final String key, final Object value) {
        this.meta.put(key, value);
    }

//    public void removeMetaInfo(final String key) {
//        this.meta.remove(key);
//    }
}
