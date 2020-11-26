package org.openscience.webcase.nmr.model.bean;

import casekit.nmr.model.DataSet;
import lombok.*;

import java.util.HashMap;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DataSetBean {

    private ExtendedConnectionMatrixBean extendedConnectionMatrixBean;
    private SpectrumBean spectrumBean;
    private AssignmentBean assignmentBean;
    private HashMap<String, String> meta;

    public DataSet toDataSet(){
        return new DataSet(this.extendedConnectionMatrixBean.toAtomContainer(), this.spectrumBean.toSpectrum(), this.assignmentBean.toAssignment(), this.meta);
    }

    public void addMetaInfo(final String key, final String value){
        this.meta.put(key, value);
    }

    public void removeMetaInfo(final String key){
        this.meta.remove(key);
    }
}
