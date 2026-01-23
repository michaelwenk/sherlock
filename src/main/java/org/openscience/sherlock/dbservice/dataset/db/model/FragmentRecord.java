package org.openscience.sherlock.dbservice.dataset.db.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.openscience.sherlock.dbservice.dataset.config.DatasetJpaConfig;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@NoArgsConstructor
@Getter
@Setter
@ToString

@Entity
@Table(name = DatasetJpaConfig.FRAGMENT_TABLE_NAME)
public class FragmentRecord {

    @Id
    private long id;
    private String nucleus;
    @Column(columnDefinition = "TEXT")
    private String subDataSetString;
}
