package org.openscience.webcase.dbservice.dataset.model.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.openscience.webcase.dbservice.dataset.model.DataSet;

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
