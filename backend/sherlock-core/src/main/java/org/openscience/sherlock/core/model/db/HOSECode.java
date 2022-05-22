package org.openscience.sherlock.core.model.db;

import lombok.*;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class HOSECode {

    private String HOSECode;
    private Map<String, Double[]> values;
}
