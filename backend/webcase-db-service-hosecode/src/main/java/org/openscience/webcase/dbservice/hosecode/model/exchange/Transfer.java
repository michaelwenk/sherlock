package org.openscience.webcase.dbservice.hosecode.model.exchange;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.nmrdisplayer.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openscience.webcase.dbservice.hosecode.model.ElucidationOptions;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class Transfer {
    // for SDF parsing
    private List<DataSet> dataSetList;
    private Data data;
    private String fileContent;
    private ElucidationOptions elucidationOptions;

    // for HOSE code statistics
    private Map<String, Map<String, Double[]>> hoseCodeShiftStatistics;
}
