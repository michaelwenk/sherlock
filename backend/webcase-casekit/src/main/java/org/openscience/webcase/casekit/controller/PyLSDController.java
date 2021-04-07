package org.openscience.webcase.casekit.controller;

import casekit.nmr.lsd.PyLSDInputFileBuilder;
import casekit.nmr.lsd.model.ElucidationOptions;
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
        final ElucidationOptions elucidationOptions = new ElucidationOptions();
        elucidationOptions.setFilterPaths(requestTransfer.getElucidationOptions()
                                                         .getFilterPaths());
        elucidationOptions.setAllowHeteroHeteroBonds(requestTransfer.getElucidationOptions()
                                                                    .isAllowHeteroHeteroBonds());
        elucidationOptions.setUseElim(requestTransfer.getElucidationOptions()
                                                     .isUseElim());
        elucidationOptions.setElimP1(requestTransfer.getElucidationOptions()
                                                    .getElimP1());
        elucidationOptions.setElimP2(requestTransfer.getElucidationOptions()
                                                    .getElimP2());

        return PyLSDInputFileBuilder.buildPyLSDInputFileContent(requestTransfer.getData(), requestTransfer.getMf(),
                                                                requestTransfer.getDetectedHybridizations(),
                                                                elucidationOptions);
    }
}
