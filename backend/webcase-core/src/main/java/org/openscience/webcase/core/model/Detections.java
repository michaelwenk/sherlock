package org.openscience.webcase.core.model;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Detections {

    private Map<Integer, List<Integer>> detectedHybridizations;
    private Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedConnectivities;
    private Map<Integer, Map<String, Map<Integer, Set<Integer>>>> forbiddenNeighbors;
    private Map<Integer, Map<String, Map<Integer, Set<Integer>>>> setNeighbors;
}
