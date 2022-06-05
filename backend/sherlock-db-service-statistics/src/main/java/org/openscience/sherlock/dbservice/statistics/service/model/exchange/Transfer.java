package org.openscience.sherlock.dbservice.statistics.service.model.exchange;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    private List<String> smilesList;
    private Spectrum querySpectrum;
    private int maxSphere;
    private double shiftTolerance;
    private double maximumAverageDeviation;
    private boolean checkMultiplicity;
    private boolean checkEquivalencesCount;
    private boolean allowLowerEquivalencesCount;
    private Detections detections;
    private MultiplicitySectionsBuilder multiplicitySectionsBuilder;
    // after prediction
    private List<DataSet> dataSetList;
}
