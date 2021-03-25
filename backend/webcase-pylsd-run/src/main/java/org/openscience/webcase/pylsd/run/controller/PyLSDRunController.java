package org.openscience.webcase.pylsd.run.controller;

import org.openscience.webcase.pylsd.run.model.exchange.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping(value = "/")
public class PyLSDRunController {

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
}
