package org.openscience.webcase.casekit.model.exchange;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.openscience.webcase.casekit.model.DereplicationOptions;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private List<DataSet> dataSetList;
    private Spectrum querySpectrum;
    private DereplicationOptions dereplicationOptions;
}
