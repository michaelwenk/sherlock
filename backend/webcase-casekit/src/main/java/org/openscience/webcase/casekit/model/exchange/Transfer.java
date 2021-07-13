package org.openscience.webcase.casekit.model.exchange;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrdisplayer.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.openscience.webcase.casekit.model.DereplicationOptions;
import org.openscience.webcase.casekit.model.ElucidationOptions;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private List<DataSet> dataSetList;
    private Spectrum querySpectrum;
    private String queryType;
    private DereplicationOptions dereplicationOptions;
    private ElucidationOptions elucidationOptions;
    private Data data;
    private String mf;
    private Map<Integer, List<Integer>> detectedHybridizations;

    // for building HOSE code statistics
    private Map<String, Map<String, Double[]>> hoseCodeShiftStatistics;

    // for ranked SDF parsing
    @Deprecated
    private String fileContent;
    @Deprecated
    private String nucleus;
    @Deprecated
    private double maxAverageDeviation;
}
