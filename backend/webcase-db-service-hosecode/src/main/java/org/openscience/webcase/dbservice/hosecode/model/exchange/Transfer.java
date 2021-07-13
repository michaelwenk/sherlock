package org.openscience.webcase.dbservice.hosecode.model.exchange;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.nmrdisplayer.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class Transfer {

    private List<DataSet> dataSetList;
    private Data data;

    // for SDF parsing
    private String fileContent;
    private double maxAverageDeviation;

    // for HOSE code statistics
    private Map<String, Map<String, Double[]>> hoseCodeShiftStatistics;
}
