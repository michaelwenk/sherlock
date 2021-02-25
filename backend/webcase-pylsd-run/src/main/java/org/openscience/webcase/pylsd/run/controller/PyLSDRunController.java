package org.openscience.webcase.pylsd.run.controller;

import org.openscience.webcase.pylsd.run.model.exchange.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping(value = "/")
public class PyLSDRunController {

    @GetMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestParam final String pathToPyLSDExecutableFolder,
                                             @RequestParam final String pathToPyLSDInputFileFolder,
                                             @RequestParam final String pathToPyLSDInputFile,
                                             @RequestParam final String requestID) {
        boolean pyLSDRunWasSuccessful;

        try {
            // try to execute PyLSD
            final ProcessBuilder builder = new ProcessBuilder();
            builder.directory(new File(pathToPyLSDExecutableFolder))
                   .redirectError(new File(pathToPyLSDInputFileFolder
                                                   + requestID
                                                   + "_error.txt"))
                   .redirectOutput(new File(pathToPyLSDInputFileFolder
                                                    + requestID
                                                    + "_log.txt"))
                   .command("python2.7", pathToPyLSDExecutableFolder
                           + "lsd_modified.py", pathToPyLSDInputFile);
            final Process process = builder.start();
            final int exitCode = process.waitFor();
            pyLSDRunWasSuccessful = exitCode
                    == 0;

            if (pyLSDRunWasSuccessful) {
                System.out.println("run was successful");
                System.out.println(pathToPyLSDInputFileFolder);
                Files.list(Paths.get(pathToPyLSDInputFileFolder))
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

                final Path resultsFilePath = Paths.get(pathToPyLSDInputFileFolder
                                                               + "/"
                                                               + requestID
                                                               + "_0.sdf");
                Files.move(resultsFilePath, resultsFilePath.resolveSibling(pathToPyLSDInputFileFolder
                                                                                   + "/"
                                                                                   + requestID
                                                                                   + ".smiles"));
            } else {
                System.out.println("run was NOT successful");
            }

        } catch (final Exception e) {
            System.out.println(e.getStackTrace());
            pyLSDRunWasSuccessful = false;
        }
        final Transfer resultTransfer = new Transfer();
        resultTransfer.setPyLSDRunWasSuccessful(pyLSDRunWasSuccessful);

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
