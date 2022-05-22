package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.db.model.FragmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FragmentRepository
        extends JpaRepository<FragmentRecord, Long> {

    // SELECT COUNT(set_bits) FROM fragment WHERE nucleus = '13C' AND set_bits <@ ARRAY[71, 98, 146];
    @Query(value = "SELECT * FROM fragment_record WHERE nucleus = ?1 AND signal_count <= ?2 AND set_bits <@ CAST(?3 AS integer[])", nativeQuery = true)
    List<FragmentRecord> findByNucleusAndSignalCountAndSetBits(final String nucleus, final int signalCount,
                                                               final String setBitsString);
}
