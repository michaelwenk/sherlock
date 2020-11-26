package org.openscience.webcase.nmr.model.bean;

import casekit.nmr.model.Assignment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AssignmentBean {

    private String[] nuclei;
    private int[][] assignments;

    public Assignment toAssignment(){
        return new Assignment(this.nuclei, this.assignments);
    }
}
