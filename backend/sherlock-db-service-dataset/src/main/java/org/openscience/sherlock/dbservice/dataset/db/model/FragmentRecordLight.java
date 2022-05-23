package org.openscience.sherlock.dbservice.dataset.db.model;

import com.vladmihalcea.hibernate.type.array.IntArrayType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

@Entity
@TypeDef(name = "int-array", typeClass = IntArrayType.class)
public class FragmentRecordLight {

    @Id
    @Column(name = "id")
    private long id;

    @Type(type = "int-array")
    @Column(name = "set_bits", columnDefinition = "integer[]")
    private int[] setBits;
}
