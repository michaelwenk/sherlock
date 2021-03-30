package org.openscience.webcase.casekit.controller;

import casekit.nmr.lsd.PyLSDInputFileBuilder;
import org.openscience.webcase.casekit.model.exchange.Transfer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/pylsd")
public class PyLSDController {

    @PostMapping(value = "/createInputFile", consumes = "application/json")
    public String createInputFile(@RequestBody final Transfer requestTransfer) {
        return PyLSDInputFileBuilder.buildPyLSDInputFileContent(requestTransfer.getData(),
                                                                requestTransfer.getElucidationOptions()
                                                                               .getMf(),
                                                                requestTransfer.getDetectedHybridizations(),
                                                                requestTransfer.getElucidationOptions()
                                                                               .isAllowHeteroHeteroBonds(),
                                                                requestTransfer.getElucidationOptions()
                                                                               .getPathToLSDFilterList());
    }
}
