package org.openscience.webcase.nmrshiftdb.service;

import org.openscience.webcase.nmrshiftdb.model.NMRShiftDBRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NMRShiftDBServiceImplementation implements NMRShiftDBService{

    private final NMRShiftDBRepository nmrShiftDBRepository;

    @Autowired
    public NMRShiftDBServiceImplementation(NMRShiftDBRepository nmrShiftDBRepository) {
        this.nmrShiftDBRepository = nmrShiftDBRepository;
    }

    @Override
    public long count() {
        return this.nmrShiftDBRepository.count();
    }

    @Override
    public void insert(final NMRShiftDBRecord nmrShiftDBRecord) {
        this.nmrShiftDBRepository.insert(nmrShiftDBRecord);
    }

    @Override
    public List<NMRShiftDBRecord> findAll() {
        return this.nmrShiftDBRepository.findAll();
    }

    @Override
    public Optional<NMRShiftDBRecord> findById(final String id) {
        return this.nmrShiftDBRepository.findById(id);
    }

    @Override
    public List<NMRShiftDBRecord> findByMf(final String mf) {
        return this.nmrShiftDBRepository.findByMf(mf);
    }

    @Override
    public List<NMRShiftDBRecord> findByDataSetBeanSpectrumBeanNucleiAndDataSetBeanSpectrumBeanSignalCount(final String[] nuclei, final int signalCount) {
        return this.nmrShiftDBRepository.findByDataSetBeanSpectrumBeanNucleiAndDataSetBeanSpectrumBeanSignalCount(nuclei, signalCount);
    }

    @Override
    public void deleteAll() {
        this.nmrShiftDBRepository.deleteAll();
    }
}
