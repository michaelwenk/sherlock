package org.openscience.webcase.pylsd.run.controller;

import org.openscience.webcase.pylsd.run.model.exchange.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping(value = "/")
public class PyLSDRunController {

    @GetMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestParam final String pathToPyLSDExecutableFolder, @RequestParam final String pathToPyLSDLogAndErrorFolder, @RequestParam final String pathToPyLSDInputFile, @RequestParam final String requestID){
        boolean pyLSDRunWasSuccessful;

        try {
            // try to execute PyLSD
            final ProcessBuilder builder = new ProcessBuilder();
            builder
                    .directory(new File(pathToPyLSDExecutableFolder))
                    .redirectError(new File(pathToPyLSDLogAndErrorFolder + "/" + "webcase_" + requestID + "_error.txt"))
                    .redirectOutput(new File(pathToPyLSDLogAndErrorFolder + "/" + "webcase_" + requestID + "_log.txt"))
                    .command("python2.7", pathToPyLSDExecutableFolder + "lsd.py", pathToPyLSDInputFile);
            final Process process = builder.start();
            final int exitCode = process.waitFor();
            pyLSDRunWasSuccessful = exitCode == 0 ? true: false;
        } catch (final Exception e){
            System.out.println(e.getStackTrace());
            pyLSDRunWasSuccessful = false;
        }
        final Transfer resultTransfer = new Transfer();
        resultTransfer.setPyLSDRunWasSuccessful(pyLSDRunWasSuccessful);

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
