package org.openscience.sherlock.dbservice.dataset.db.service;

import org.openscience.sherlock.dbservice.dataset.db.model.FragmentLookupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FragmentLookupRepository
        extends JpaRepository<FragmentLookupRecord, Long> {

    @Query(value = "SELECT * FROM fragment_lookup_record WHERE set_bits <@ CAST(?1 AS integer[])", nativeQuery = true)
    List<FragmentLookupRecord> findByNucleusAndSetBits(final String setBitsString);
}
