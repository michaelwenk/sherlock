package org.openscience.webcase.casekit.model.exchange;

import casekit.nmr.model.DataSet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private List<DataSet> dataSetList;
}
