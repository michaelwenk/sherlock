package org.openscience.sherlock.dbservice.dataset.model.exchange;

import casekit.nmr.model.Spectrum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private Spectrum querySpectrum;
    private String mf;
    private List<List<Integer>> hybridizationList;
    private double shiftTolerance;
    private double maximumAverageDeviation;
    private int nThreads;
}
