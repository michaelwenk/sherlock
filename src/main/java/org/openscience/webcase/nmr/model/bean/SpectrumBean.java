package org.openscience.webcase.nmr.model.bean;

import casekit.nmr.model.Spectrum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SpectrumBean {

    private String[] nuclei;
    private String description;
    private String specType;
    private Double spectrometerFrequency;
    private String solvent;
    private String standard;
    private ArrayList<SignalBean> signals;
    private int signalCount;
    private ArrayList<Integer> equivalences;
    private  ArrayList<Integer>[] equivalentSignals;

    public Spectrum toSpectrum(){
        return new Spectrum(this.nuclei, this.description, this.specType, this.spectrometerFrequency, this.solvent, this.standard, this.signals.stream().map(SignalBean::toSignal).collect(Collectors.toList()), this.signalCount, this.equivalences, this.equivalentSignals);
    }
}
