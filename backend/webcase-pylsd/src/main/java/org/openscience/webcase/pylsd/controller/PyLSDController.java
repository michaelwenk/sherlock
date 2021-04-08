package org.openscience.webcase.pylsd.controller;

import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.openscience.webcase.pylsd.model.nmrdisplayer.Data;
import org.openscience.webcase.pylsd.utils.FileSystem;
import org.openscience.webcase.pylsd.utils.HybridizationDetection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class PyLSDController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "createPyLSDInputFile", consumes = "application/json")
    public ResponseEntity<Transfer> createPyLSDInputFile(@RequestBody final Transfer requestTransfer) {
        final Data data = requestTransfer.getData();

        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.getDetectedHybridizations(
                this.webClientBuilder, data, requestTransfer.getElucidationOptions()
                                                            .getHybridizationDetectionThreshold());

        final Transfer resultTransfer = new Transfer();
        try {
            System.out.println(requestTransfer.getElucidationOptions()
                                              .getPathToPyLSDInputFileFolder());
            final Path path = Paths.get(requestTransfer.getElucidationOptions()
                                                       .getPathToPyLSDInputFileFolder());
            Files.createDirectory(path);
            System.out.println("Directory is created!");

            final WebClient webClient = this.webClientBuilder.
                                                                     baseUrl("http://localhost:8081/webcase-casekit/pylsd/createInputFile")
                                                             .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                            MediaType.APPLICATION_JSON_VALUE)
                                                             .build();
            final Transfer queryTransfer = new Transfer();
            queryTransfer.setData(requestTransfer.getData());
            queryTransfer.setDetectedHybridizations(detectedHybridizations);
            queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
            queryTransfer.setMf(requestTransfer.getMf());
            final String pyLSDInputFileContent = webClient.post()
                                                          .bodyValue(queryTransfer)
                                                          .retrieve()
                                                          .bodyToMono(String.class)
                                                          .block();
            resultTransfer.setPyLSDInputFileCreationWasSuccessful(FileSystem.writeFile(
                    requestTransfer.getElucidationOptions()
                                   .getPathToPyLSDInputFile(), pyLSDInputFileContent));
        } catch (final IOException e) {
            System.err.println("Failed to create directory!"
                                       + e.getMessage());
            resultTransfer.setPyLSDInputFileCreationWasSuccessful(false);
        }

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    @PostMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestBody final Transfer requestTransfer) {
        final Transfer resultTransfer = new Transfer();

        try {
            // try to execute PyLSD
            final ProcessBuilder builder = new ProcessBuilder();
            builder.directory(new File(requestTransfer.getElucidationOptions()
                                                      .getPathToPyLSDExecutableFolder()))
                   .redirectError(new File(requestTransfer.getElucidationOptions()
                                                          .getPathToPyLSDInputFileFolder()
                                                   + requestTransfer.getRequestID()
                                                   + "_error.txt"))
                   .redirectOutput(new File(requestTransfer.getElucidationOptions()
                                                           .getPathToPyLSDInputFileFolder()
                                                    + requestTransfer.getRequestID()
                                                    + "_log.txt"))
                   .command("python2.7", requestTransfer.getElucidationOptions()
                                                        .getPathToPyLSDExecutableFolder()
                           + "lsd_modified.py", requestTransfer.getElucidationOptions()
                                                               .getPathToPyLSDInputFile());
            final Process process = builder.start();
            final int exitCode = process.waitFor();
            final boolean pyLSDRunWasSuccessful = exitCode
                    == 0;

            if (pyLSDRunWasSuccessful) {
                System.out.println("run was successful");
                System.out.println(requestTransfer.getElucidationOptions()
                                                  .getPathToPyLSDInputFileFolder());
                resultTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
                final String pathToResultsFilePredictions = requestTransfer.getElucidationOptions()
                                                                           .getPathToPyLSDInputFileFolder()
                        + "/"
                        + requestTransfer.getRequestID()
                        + "_D.sdf";
                resultTransfer.getElucidationOptions()
                              .setPathToResultsFile(pathToResultsFilePredictions);
            } else {
                System.out.println("run was NOT successful");
            }
            resultTransfer.setPyLSDRunWasSuccessful(pyLSDRunWasSuccessful);

        } catch (final Exception e) {
            e.printStackTrace();
            resultTransfer.setPyLSDRunWasSuccessful(false);
        }

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "/cleanup")
    public ResponseEntity<Boolean> cleanup(@RequestParam final String pathToPyLSDInputFileFolder) {
        boolean cleaned = false;
        final Path path = Paths.get(pathToPyLSDInputFileFolder);
        try {
            Files.walk(path)
                 .map(Path::toFile)
                 .forEach(File::delete);
            Files.delete(path);
            if (!Files.exists(path)) {
                System.out.println("Directory is deleted!");
                cleaned = true;
            }
        } catch (final IOException e) {
            System.out.println("Directory NOT deleted!");
            e.printStackTrace();
        }
        return new ResponseEntity<>(cleaned, HttpStatus.OK);
    }
}
