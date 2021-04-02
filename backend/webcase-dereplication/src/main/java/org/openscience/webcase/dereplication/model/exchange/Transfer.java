package org.openscience.webcase.dereplication.model.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.openscience.webcase.dereplication.model.DataSet;
import org.openscience.webcase.dereplication.model.DereplicationOptions;
import org.openscience.webcase.dereplication.model.Spectrum;

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
    private String queryType;
    private String mf;

    private DereplicationOptions dereplicationOptions;
}
