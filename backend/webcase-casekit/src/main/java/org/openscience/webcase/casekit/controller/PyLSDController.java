package org.openscience.webcase.casekit.controller;

import casekit.nmr.lsd.PyLSDInputFileBuilder;
import casekit.nmr.lsd.model.ElucidationOptions;
import org.openscience.webcase.casekit.model.exchange.Transfer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/pylsd")
public class PyLSDController {

    @PostMapping(value = "/createInputFile", consumes = "application/json")
    public String createInputFile(@RequestBody final Transfer requestTransfer) {
        final ElucidationOptions elucidationOptions = new ElucidationOptions();

        final Path pathToFilters = Paths.get("/data/lsd/filters/");
        List<String> filterList = new ArrayList<>();
        try {
            filterList = Files.walk(pathToFilters)
                              .filter(path -> !Files.isDirectory(path))
                              .map(path -> path.toFile()
                                               .getAbsolutePath())
                              .collect(Collectors.toList());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        System.out.println(Arrays.toString(filterList.toArray(String[]::new)));
        elucidationOptions.setFilterPaths(filterList.toArray(String[]::new));
        elucidationOptions.setAllowHeteroHeteroBonds(requestTransfer.getElucidationOptions()
                                                                    .isAllowHeteroHeteroBonds());
        elucidationOptions.setUseElim(requestTransfer.getElucidationOptions()
                                                     .isUseElim());
        elucidationOptions.setElimP1(requestTransfer.getElucidationOptions()
                                                    .getElimP1());
        elucidationOptions.setElimP2(requestTransfer.getElucidationOptions()
                                                    .getElimP2());
        elucidationOptions.setHmbcP3(requestTransfer.getElucidationOptions()
                                                    .getHmbcP3());
        elucidationOptions.setHmbcP4(requestTransfer.getElucidationOptions()
                                                    .getHmbcP4());
        elucidationOptions.setCosyP3(requestTransfer.getElucidationOptions()
                                                    .getCosyP3());
        elucidationOptions.setCosyP4(requestTransfer.getElucidationOptions()
                                                    .getCosyP4());

        return PyLSDInputFileBuilder.buildPyLSDInputFileContent(requestTransfer.getData(), requestTransfer.getMf(),
                                                                requestTransfer.getDetectedHybridizations(),
                                                                elucidationOptions);
    }
}
