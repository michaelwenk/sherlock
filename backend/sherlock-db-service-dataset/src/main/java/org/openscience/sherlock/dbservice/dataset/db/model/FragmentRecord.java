package org.openscience.sherlock.dbservice.dataset.db.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;

@NoArgsConstructor
@Getter
@Setter
@ToString

@Entity
public class FragmentRecord {

    @Id
    private long id;
    private String nucleus;
    private String setBits;
    private long nBits;
    private String subDataSetString;
}
