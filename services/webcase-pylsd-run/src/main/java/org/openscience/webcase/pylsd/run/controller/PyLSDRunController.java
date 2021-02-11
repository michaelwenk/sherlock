package org.openscience.webcase.pylsd.run.controller;

import org.openscience.webcase.pylsd.run.model.exchange.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/")
public class PyLSDRunController {

    @GetMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestBody final Transfer requestTransfer, @RequestParam final String pathToPyLSDFileFolder){
        final Transfer resultTransfer = new Transfer();

        final String pyLSDOutputFileContent = "";

        resultTransfer.setPyLSDOutputFileContent(pyLSDOutputFileContent);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
