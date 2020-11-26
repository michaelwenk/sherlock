package org.openscience.webcase.nmrshiftdb.model;

import lombok.*;
import org.openscience.webcase.nmr.model.bean.DataSetBean;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class NMRShiftDBRecordBean {

    private String id;
    @NotNull
    private String mf;
    @NotNull
    private DataSetBean dataSet;
}
