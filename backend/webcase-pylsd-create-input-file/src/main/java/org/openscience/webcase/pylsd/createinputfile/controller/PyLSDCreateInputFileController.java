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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public ResponseEntity<Transfer> createPyLSDInputFile(@RequestBody final Transfer requestTransfer, @RequestParam final boolean allowHeteroHeteroBonds, @RequestParam final String pathToPyLSDInputFileFolder, @RequestParam final String pathToPyLSDInputFile, @RequestParam final String pathToLSDFilterList, @RequestParam final String requestID){
        final Data data = requestTransfer.getData();
        final String mf = (String) data.getCorrelations().getOptions().get("mf");
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.getDetectedHybridizations(webClientBuilder, data, thrsHybridizations);

        final Transfer resultTransfer = new Transfer();
        try {
            final Path path = Paths.get(pathToPyLSDInputFileFolder);
            Files.createDirectory(path);
            System.out.println("Directory is created!");
            final String pyLSDInputFileContent = PyLSDInputFileBuilder.buildPyLSDInputFileContent(data, mf, detectedHybridizations, allowHeteroHeteroBonds, pathToLSDFilterList, requestID);
            resultTransfer.setPyLSDInputFileCreationWasSuccessful(PyLSDInputFileBuilder.writePyLSDInputFileContentToFile(pathToPyLSDInputFile, pyLSDInputFileContent));
        } catch (final IOException e) {
            System.err.println("Failed to create directory!" + e.getMessage());
            resultTransfer.setPyLSDInputFileCreationWasSuccessful(false);
        }

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
