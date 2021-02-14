package org.openscience.webcase.pylsd.createinputfile.controller;

import org.openscience.webcase.pylsd.createinputfile.model.exchange.Transfer;
import org.openscience.webcase.pylsd.createinputfile.model.nmrdisplayer.Data;
import org.openscience.webcase.pylsd.createinputfile.utils.HybridizationDetection;
import org.openscience.webcase.pylsd.createinputfile.utils.PyLSDInputFileBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class PyLSDCreateInputFileController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    // @TODO get as parameter from somewhere
    final double thrsHybridizations = 0.1; // threshold to take a hybridization into account

    @PostMapping(value = "createPyLSDInputFile", consumes = "application/json")
    public ResponseEntity<Transfer> createPyLSDInputFile(@RequestBody final Transfer requestTransfer, @RequestParam final boolean allowHeteroHeteroBonds, @RequestParam final String pathToPyLSDInputFile, @RequestParam final String pathToLSDFilterList, @RequestParam final String requestID){
        final Data data = requestTransfer.getData();
        final String mf = (String) data.getCorrelations().getOptions().get("mf");
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.getDetectedHybridizations(webClientBuilder, data, thrsHybridizations);
        final String pyLSDInputFileContent = PyLSDInputFileBuilder.buildPyLSDInputFileContent(data, mf, detectedHybridizations, allowHeteroHeteroBonds, pathToLSDFilterList, requestID);

        final Transfer resultTransfer = new Transfer();
        resultTransfer.setPyLSDInputFileCreationWasSuccessful(PyLSDInputFileBuilder.writePyLSDInputFileContentToFile(pathToPyLSDInputFile, pyLSDInputFileContent));
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
