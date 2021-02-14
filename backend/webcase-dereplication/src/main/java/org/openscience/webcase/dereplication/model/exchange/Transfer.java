package org.openscience.webcase.dereplication.model.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.openscience.webcase.dereplication.model.DataSet;
import org.openscience.webcase.dereplication.model.Spectrum;
import org.openscience.webcase.dereplication.model.nmrdisplayer.Data;

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
    private Data data;
    private Spectrum querySpectrum;
    private Map<String, Double> shiftTolerances;
}
