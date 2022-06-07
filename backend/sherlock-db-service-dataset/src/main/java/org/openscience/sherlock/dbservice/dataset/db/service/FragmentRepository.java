package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.db.model.FragmentRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.FragmentRecordLight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FragmentRepository
        extends JpaRepository<FragmentRecord, Long> {


    // manual create of table was needed which includes the SERIAL keyword to auto-increment
    // CREATE TABLE fragment_record(id SERIAL PRIMARY KEY NOT NULL, nucleus VARCHAR(5) NOT NULL, signal_count INTEGER NOT NULL, set_bits BIT(209) NOT NULL, n_bits INTEGER NOT NULL, sub_data_set_string TEXT);
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO fragment_record(nucleus, signal_count, set_bits, n_bits, sub_data_set_string) VALUES (?1, ?2, CAST(?3 AS BIT(209)), ?4, ?5);", nativeQuery = true)
    void insertFragmentRecord(final String nucleus, final int signalCount, final String setBits, final int nBits,
                              final String subDataSetString);


    //    // SELECT COUNT(set_bits) FROM fragment WHERE nucleus = '13C' AND set_bits <@ ARRAY[71, 98, 146];
    //    @Query(value = "SELECT * FROM fragment_record WHERE nucleus = ?1 AND signal_count <= ?2 AND set_bits <@ CAST(?3 AS integer[])", nativeQuery = true)
    //    List<FragmentRecord> findByNucleusAndSignalCountAndSetBits(final String nucleus, final int signalCount,
    //                                                               final String setBitsString);

    @Query(value = "SELECT id, sub_data_set_string as subDataSetString  FROM fragment_record WHERE set_bits & CAST(CAST(?1 AS TEXT) AS BIT(209)) = set_bits;", nativeQuery = true)
    List<FragmentRecordLight> findBySetBits(final String bitsString);
}
