package org.openscience.webcase.dbservicehybridizations.controller;

import org.openscience.webcase.dbservicehybridizations.model.HybridizationRecord;
import org.openscience.webcase.dbservicehybridizations.service.HybridizationServiceImplementation;
import org.openscience.webcase.dbservicehybridizations.utils.Constants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/nmrshiftdb")
public class HybridizationsController {

    private final HybridizationServiceImplementation hybridizationServiceImplementation;

    public HybridizationsController(final HybridizationServiceImplementation hybridizationServiceImplementation) {
        this.hybridizationServiceImplementation = hybridizationServiceImplementation;
    }


    @GetMapping(value = "/getAll", produces = "application/json")
    public List<HybridizationRecord> getHybridizationCollection() {
        return this.hybridizationServiceImplementation.findAll();
    }

    @GetMapping(value = "/detectHybridizations", produces = "application/json")
    public List<Integer> detectHybridization(@RequestParam final String nucleus, @RequestParam final int minShift, @RequestParam final int maxShift, @RequestParam final String multiplicity, @RequestParam final double thrs) {
        final List<String> hybridizations = this.hybridizationServiceImplementation.aggregateHybridizationsByNucleusAndShiftAndMultiplicity(nucleus, minShift, maxShift, multiplicity);
        final Set<String> uniqueLabels = new HashSet<>(hybridizations);
        final Set<Integer> uniqueValues = new HashSet<>();

        uniqueLabels.forEach(label -> {
            if (Constants.hybridizationConversionMap.containsKey(nucleus) && Constants.hybridizationConversionMap.get(nucleus).containsKey(label) && hybridizations.stream().filter(value -> value.equals(label)).count() / (double) hybridizations.size() >= thrs) {
                uniqueValues.add(Constants.hybridizationConversionMap.get(nucleus).get(label));
            }
        });
        
        return new ArrayList<>(uniqueValues);
    }

//    @GetMapping(value = "/buildHybridizationCollection")
//    public void buildHybridizationCollection() {
//
//        this.hybridizationRepository.deleteAll();
//
//        final String[] nuclei = new String[]{"13C", "15N"};
//        DataSet dataSet;
//        String atomType;
//        IAtomType.Hybridization hybridization;
//        String multiplicity;
//        Integer shift;
//        for (final String nucleus : nuclei) {
//            final List<DataSetRecord> dataSetRecords = this.nmrShiftDBController.getByDataSetSpectrumNuclei(new String[]{nucleus});
//            atomType = Utils.getAtomTypeFromNucleus(nucleus);
//            for (DataSetRecord dataSetRecord : dataSetRecords) {
//                dataSet = dataSetRecord.getDataSet();
//                for (int i = 0; i < dataSet.getAssignment().getAssignmentsCount(); i++) {
//                    hybridization = dataSet.getStructure().getHybridization(dataSet.getAssignment().getAssignment(0, i));
//                    multiplicity = dataSet.getSpectrum().getMultiplicity(i);
//                    shift = null;
//
//                    if (dataSet.getSpectrum().getShift(i, 0) != null) {
//                        shift = dataSet.getSpectrum().getShift(i, 0).intValue();
//                    }
//                    if (shift == null || dataSet.getStructure().getAtomType(dataSet.getAssignment().getAssignment(0, i)) == null || !dataSet.getStructure().getAtomType(dataSet.getAssignment().getAssignment(0, i)).equals(atomType)) {
//                        continue;
//                    }
//                    this.hybridizationRepository.insert(new HybridizationRecord(null, nucleus, shift, multiplicity, hybridization.name()));
//                }
//            }
//        }
//    }
}
