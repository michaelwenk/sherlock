package org.openscience.sherlock.dbservice.dataset.model.exchange;

import casekit.nmr.model.Spectrum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    final Spectrum querySpectrum;
    final String mf;
    final List<List<Integer>> hybridizationList;
    final double shiftTolerance;
    final double maximumAverageDeviation;
}
