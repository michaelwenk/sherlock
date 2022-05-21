package org.openscience.sherlock.dbservice.dataset.db.model;

import com.vladmihalcea.hibernate.type.array.IntArrayType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

@Entity
@Table
@TypeDef(name = "int-array", typeClass = IntArrayType.class)
public class FragmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private long id;

    @Column(name = "mf", nullable = false)
    private String mf;

    @Column(name = "smiles", nullable = false, columnDefinition = "TEXT")
    private String smiles;

    @Column(name = "nucleus")
    private String nucleus;

    @Column(name = "signal_count", nullable = false)
    private int signalCount;

    @Type(type = "int-array")
    @Column(name = "set_bits", columnDefinition = "integer[]")
    private int[] setBits;

    @Column(name = "fp_size", nullable = false)
    private long fpSize;

    @Column(name = "sub_data_set_string", nullable = false, columnDefinition = "TEXT")
    private String subDataSetString;
}
