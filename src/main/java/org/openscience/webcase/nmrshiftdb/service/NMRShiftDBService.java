package org.openscience.webcase.nmrshiftdb.service;

import org.openscience.webcase.nmrshiftdb.model.NMRShiftDBRecord;

import java.util.List;
import java.util.Optional;

public interface NMRShiftDBService {

    long count();
    void insert(final NMRShiftDBRecord nmrShiftDBRecord);
    List<NMRShiftDBRecord> findAll();
    Optional<NMRShiftDBRecord> findById(final String id);
    List<NMRShiftDBRecord> findByMf(final String mf);
    List<NMRShiftDBRecord> findByDataSetBeanSpectrumBeanNucleiAndDataSetBeanSpectrumBeanSignalCount(final String[] nuclei, final int signalCount);

    void deleteAll();

}
