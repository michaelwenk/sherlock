package org.openscience.sherlock.dbservice.dataset.db.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Setter
@ToString

@Entity
@Table
public class FragmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private long id;

    @Column(name = "nucleus", nullable = false)
    private String nucleus;

    @Column(name = "signal_count", nullable = false)
    private int signalCount;

    @Column(name = "set_bits", nullable = false, columnDefinition = "BIT(209)")
    private String setBits;

    @Column(name = "n_bits", nullable = false)
    private long nBits;

    @Column(name = "sub_data_set_string", nullable = false, columnDefinition = "TEXT")
    private String subDataSetString;
}
