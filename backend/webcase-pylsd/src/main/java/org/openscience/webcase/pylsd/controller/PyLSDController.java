package org.openscience.webcase.pylsd.controller;

import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.openscience.webcase.pylsd.model.nmrdisplayer.Data;
import org.openscience.webcase.pylsd.utils.HybridizationDetection;
import org.openscience.webcase.pylsd.utils.PyLSDInputFileBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
                Files.list(Paths.get(requestTransfer.getElucidationOptions()
                                                    .getPathToPyLSDInputFileFolder()))
                     .filter(file -> Files.isRegularFile(file)
                             && (file.getFileName()
                                     .toString()
                                     .endsWith(".ps")
                             || file.getFileName()
                                    .toString()
                                    .endsWith(".coo")
                             || file.getFileName()
                                    .toString()
                                    .endsWith(".sol")
                             || file.getFileName()
                                    .toString()
                                    .endsWith("_R.txt")
                             || file.getFileName()
                                    .toString()
                                    .endsWith("_C.txt")
                             || file.getFileName()
                                    .toString()
                                    .endsWith(".sol")
                             || file.getFileName()
                                    .toString()
                                    .endsWith("_D.sdf")
                             //                             || file.getFileName()
                             //                                    .toString()
                             //                                    .matches(requestID
                             //                                                     + "_"
                             //                                                     + "\\d+")
                             || file.getFileName()
                                    .toString()
                                    .endsWith(".lsd")))
                     .forEach(file -> file.toFile()
                                          .delete());

                final Path resultsFilePath = Paths.get(requestTransfer.getElucidationOptions()
                                                                      .getPathToPyLSDInputFileFolder()
                                                               + "/"
                                                               + requestTransfer.getRequestID()
                                                               + "_0.sdf");
                final String pathToResultsFile = requestTransfer.getElucidationOptions()
                                                                .getPathToPyLSDInputFileFolder()
                        + "/"
                        + requestTransfer.getRequestID()
                        + ".smiles";
                Files.move(resultsFilePath, resultsFilePath.resolveSibling(pathToResultsFile));
                resultTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
                resultTransfer.getElucidationOptions()
                              .setPathToResultsFile(pathToResultsFile);
            } else {
                System.out.println("run was NOT successful");
            }
            resultTransfer.setPyLSDRunWasSuccessful(pyLSDRunWasSuccessful);

        } catch (final Exception e) {
            System.out.println(e.getStackTrace());
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
