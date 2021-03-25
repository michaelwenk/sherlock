package org.openscience.webcase.pylsd.run.model.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.openscience.webcase.pylsd.run.model.ElucidationOptions;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private Boolean pyLSDRunWasSuccessful;
    private ElucidationOptions elucidationOptions;
    private String requestID;
}
