package org.openscience.sherlock.dbservice.statistics.service.model.exchange;

import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openscience.sherlock.dbservice.statistics.model.PredictionOptions;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    private Spectrum querySpectrum;
    private Detections detections;
    // for stereo prediction
    private PredictionOptions predictionOptions;
    private String smiles;
    // after prediction
    private List<DataSet> dataSetList;
}
