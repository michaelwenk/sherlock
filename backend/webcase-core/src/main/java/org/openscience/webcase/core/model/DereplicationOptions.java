package org.openscience.webcase.core.model;

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
