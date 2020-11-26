package org.openscience.webcase.controller;

import casekit.nmr.Utils;
import casekit.nmr.dbservice.NMRShiftDB;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.ExtendedConnectionMatrix;
import org.openscience.cdk.exception.CDKException;
import org.openscience.webcase.nmr.model.bean.*;
import org.openscience.webcase.nmrshiftdb.model.NMRShiftDBRecord;
import org.openscience.webcase.nmrshiftdb.model.NMRShiftDBRecordBean;
import org.openscience.webcase.nmrshiftdb.service.NMRShiftDBServiceImplementation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/nmrshiftdb")
public class NMRShiftDBController {

//    private final NMRShiftDBRepository nmrShiftDBRepository;
    private final NMRShiftDBServiceImplementation nmrShiftDBRepository;

    @Autowired
    public NMRShiftDBController(final NMRShiftDBServiceImplementation nmrShiftDBRepository) {
        this.nmrShiftDBRepository = nmrShiftDBRepository;
    }

    @GetMapping(value = "/count")
    public String getCount() {
        return String.valueOf(this.nmrShiftDBRepository.count());
    }

    @GetMapping(value = "/get/all")
    public List<NMRShiftDBRecord> getAll() {
       return this.nmrShiftDBRepository.findAll();
    }

    @GetMapping(value = "/get/mf", produces = "application/json")
    public List<NMRShiftDBRecord> getByMf(@RequestParam @Valid final String mf) {
        return this.nmrShiftDBRepository.findByMf(mf);
    }

    @GetMapping(value = "/test")
    public List<NMRShiftDBRecord> getByDataSetBeanSpectrumBeanNucleiAndDataSetBeanSpectrumBeanSignalCount(@RequestParam @Valid final String[] nuclei, @RequestParam @Valid final int signalCount ) {
        return this.nmrShiftDBRepository.findByDataSetBeanSpectrumBeanNucleiAndDataSetBeanSpectrumBeanSignalCount(nuclei, signalCount);
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public void insert(@RequestBody @Valid final NMRShiftDBRecordBean nmrShiftDBRecordBean){
        final NMRShiftDBRecord nmrShiftDBRecord = new NMRShiftDBRecord(
                null,
                nmrShiftDBRecordBean.getMf(),
                nmrShiftDBRecordBean.getDataSet()
        );
        this.nmrShiftDBRepository.insert(nmrShiftDBRecord);
    }

    @DeleteMapping(value = "/delete/all")
    public void deleteAll() {
        this.nmrShiftDBRepository.deleteAll();
    }

    @PostMapping(value = "/replace/all", consumes = "text/plain")
    public void replaceAll(@RequestParam @Valid final String filePath){
        this.deleteAll();

        final String spectrumProperty = "Spectrum 13C 0";
        try {
            final ArrayList<DataSet> dataSets = new ArrayList<>(NMRShiftDB.getDataSetsFromNMRShiftDB(filePath, spectrumProperty));
            dataSets.forEach(dataSet ->
                this.nmrShiftDBRepository.insert(
                        new NMRShiftDBRecord(
                                null,
                                Utils.molecularFormularToString(Utils.getMolecularFormulaFromAtomContainer(dataSet.getStructure())),
                                this.convertToDataSetBean(dataSet)
                        )
                )
            );
        } catch (FileNotFoundException | CDKException e) {
            e.printStackTrace();
        }
    }

    private DataSetBean convertToDataSetBean(final DataSet dataSet){
        final ExtendedConnectionMatrix extendedConnectionMatrixTemp = new ExtendedConnectionMatrix(dataSet.getStructure());
        final ExtendedConnectionMatrixBean extendedConnectionMatrixBean = new ExtendedConnectionMatrixBean(
                extendedConnectionMatrixTemp.getConnectionMatrix(),
                extendedConnectionMatrixTemp.getAtomTypes(),
                extendedConnectionMatrixTemp.getAtomPropertiesNumeric(),
                extendedConnectionMatrixTemp.getHybridizations(),
                extendedConnectionMatrixTemp.getAtomPropertiesBoolean(),
                extendedConnectionMatrixTemp.getBondProperties(),
                extendedConnectionMatrixTemp.getBondCount()
        );
        final SpectrumBean spectrumBean = new SpectrumBean(
                dataSet.getSpectrum().getNuclei(),
                dataSet.getSpectrum().getDescription(),
                dataSet.getSpectrum().getSpecType(),
                dataSet.getSpectrum().getSpectrometerFrequency(),
                dataSet.getSpectrum().getSolvent(),
                dataSet.getSpectrum().getStandard(),
                (ArrayList<SignalBean>) dataSet.getSpectrum().getSignals().stream().map(signal ->
                        new SignalBean(signal.getNuclei(), signal.getShifts(), signal.getMultiplicity(), signal.getIntensity()))
                        .collect(Collectors.toList()),
                dataSet.getSpectrum().getSignalCount(),
                dataSet.getSpectrum().getEquivalences(),
                dataSet.getSpectrum().getEquivalentSignals()
        );
        final AssignmentBean assignmentBean = new AssignmentBean(dataSet.getSpectrum().getNuclei(), dataSet.getAssignment().getAssignments());

        return new DataSetBean(extendedConnectionMatrixBean, spectrumBean, assignmentBean, new HashMap<>(dataSet.getMeta()));
    }
}
