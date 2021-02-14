package org.openscience.webcase.core.model;


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
    private Map<String, String> meta;

    public void addMetaInfo(final String key, final String value) {
        this.meta.put(key, value);
    }

    public void removeMetaInfo(final String key) {
        this.meta.remove(key);
    }
}
