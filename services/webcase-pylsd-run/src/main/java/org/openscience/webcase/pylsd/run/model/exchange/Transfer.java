package org.openscience.webcase.pylsd.run.model.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private String pyLSDInputFileContent;
    private String pyLSDOutputFileContent;
}
