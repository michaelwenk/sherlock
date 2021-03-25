package org.openscience.webcase.pylsd.createinputfile.controller;

import org.openscience.webcase.pylsd.createinputfile.model.exchange.Transfer;
import org.openscience.webcase.pylsd.createinputfile.model.nmrdisplayer.Data;
import org.openscience.webcase.pylsd.createinputfile.utils.HybridizationDetection;
import org.openscience.webcase.pylsd.createinputfile.utils.PyLSDInputFileBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    // @TODO get as parameter from somewhere
    final double thrsHybridizations = 0.1; // threshold to take a hybridization into account
    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "createPyLSDInputFile", consumes = "application/json")
    public ResponseEntity<Transfer> createPyLSDInputFile(@RequestBody final Transfer requestTransfer) {
        final Data data = requestTransfer.getData();
        final String mf = (String) data.getCorrelations()
                                       .getOptions()
                                       .get("mf");
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.getDetectedHybridizations(
                this.webClientBuilder, data, this.thrsHybridizations);

        final Transfer resultTransfer = new Transfer();
        try {
            System.out.println(requestTransfer.getElucidationOptions()
                                              .getPathToPyLSDInputFileFolder());
            final Path path = Paths.get(requestTransfer.getElucidationOptions()
                                                       .getPathToPyLSDInputFileFolder());
            Files.createDirectory(path);
            System.out.println("Directory is created!");
            final String pyLSDInputFileContent = PyLSDInputFileBuilder.buildPyLSDInputFileContent(data, mf,
                                                                                                  detectedHybridizations,
                                                                                                  requestTransfer.getElucidationOptions()
                                                                                                                 .isAllowHeteroHeteroBonds(),
                                                                                                  requestTransfer.getElucidationOptions()
                                                                                                                 .getPathToLSDFilterList(),
                                                                                                  requestTransfer.getRequestID());
            resultTransfer.setPyLSDInputFileCreationWasSuccessful(
                    PyLSDInputFileBuilder.writePyLSDInputFileContentToFile(requestTransfer.getElucidationOptions()
                                                                                          .getPathToPyLSDInputFile(),
                                                                           pyLSDInputFileContent));
        } catch (final IOException e) {
            System.err.println("Failed to create directory!"
                                       + e.getMessage());
            resultTransfer.setPyLSDInputFileCreationWasSuccessful(false);
        }

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
