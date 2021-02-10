package org.openscience.webcase.createpylsdinputfile.controller;

import org.openscience.webcase.createpylsdinputfile.model.exchange.Transfer;
import org.openscience.webcase.createpylsdinputfile.model.nmrdisplayer.Data;
import org.openscience.webcase.createpylsdinputfile.utils.HybridizationDetection;
import org.openscience.webcase.createpylsdinputfile.utils.PyLSDInputFileBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class CreatePyLSDInputFileController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    // @TODO get as parameter from somewhere
    final double thrsHybridizations = 0.1; // threshold to take a hybridization into account

    @PostMapping(value = "createPyLSDInputFile", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> createPyLSDInputFile(@RequestBody final Transfer requestTransfer, @RequestParam final boolean allowHeteroHeteroBonds, @RequestParam final String PATH_TO_LSD_FILTER_LIST, @RequestParam final String requestID){
        final Transfer resultTransfer = new Transfer();
        final Data data = requestTransfer.getData();
        final String mf = (String) data.getCorrelations().getOptions().get("mf");
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.getDetectedHybridizations(webClientBuilder, data, thrsHybridizations);
        final String pyLSDInputFile = PyLSDInputFileBuilder.buildPyLSDFileContent(data, mf, detectedHybridizations, allowHeteroHeteroBonds, PATH_TO_LSD_FILTER_LIST, requestID);

        resultTransfer.setPyLSDInputFile(pyLSDInputFile);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
