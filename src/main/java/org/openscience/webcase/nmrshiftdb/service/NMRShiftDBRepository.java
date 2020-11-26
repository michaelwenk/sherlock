package org.openscience.webcase.nmrshiftdb.service;

import org.openscience.webcase.nmrshiftdb.model.NMRShiftDBRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NMRShiftDBRepository extends MongoRepository<NMRShiftDBRecord, String> {

    Optional<NMRShiftDBRecord> findById(final String id);
    List<NMRShiftDBRecord> findByMf(final String mf);
    List<NMRShiftDBRecord> findByDataSetBeanSpectrumBeanNucleiAndDataSetBeanSpectrumBeanSignalCount(final String[] nuclei, final int signalCount);

}
