package org.openscience.webcase.dbservice.result.controller;

import org.openscience.webcase.dbservice.result.model.DataSet;
import org.openscience.webcase.dbservice.result.service.ResultServiceImplementation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/")
public class ResultController {

    private final ResultServiceImplementation resultServiceImplementation;

    public ResultController(final ResultServiceImplementation resultServiceImplementation) {
        this.resultServiceImplementation = resultServiceImplementation;
    }

    @GetMapping(value = "/getById", produces = "application/json")
    public List<DataSet> getById(@RequestParam final String id) {
        return this.resultServiceImplementation.findById(id);
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public String insert(@RequestBody final List<DataSet> dataSetList) {
        return this.resultServiceImplementation.insert(dataSetList);
    }
}
