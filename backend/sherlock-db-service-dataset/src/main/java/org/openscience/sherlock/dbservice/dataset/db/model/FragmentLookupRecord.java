package org.openscience.sherlock.dbservice.dataset.db.model;

import com.vladmihalcea.hibernate.type.array.IntArrayType;
import com.vladmihalcea.hibernate.type.array.LongArrayType;
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
@TypeDef(name = "long-array", typeClass = LongArrayType.class)
public class FragmentLookupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(name = "nucleus")
    private String nucleus;

    @Type(type = "int-array")
    @Column(name = "set_bits", nullable = false, columnDefinition = "integer[]")
    private int[] setBits;

    @Type(type = "long-array")
    @Column(name = "ids", nullable = false, columnDefinition = "bigint[]")
    private long[] ids;
}
