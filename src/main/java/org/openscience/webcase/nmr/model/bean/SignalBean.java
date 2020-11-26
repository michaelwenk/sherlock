package org.openscience.webcase.nmr.model.bean;

import casekit.nmr.model.Signal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SignalBean {

    private String[] nuclei;
    private Double[] shifts;
    private String multiplicity;
    private Double intensity;

    public Signal toSignal(){
        return new Signal(this.nuclei, this.shifts, this.multiplicity, this.intensity);
    }
}
