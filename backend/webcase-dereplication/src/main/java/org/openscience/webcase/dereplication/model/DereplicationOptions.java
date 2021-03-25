package org.openscience.webcase.dereplication.model;

import lombok.*;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DereplicationOptions {
    private Map<String, Double> shiftTolerances;
}
