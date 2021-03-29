package org.openscience.webcase.dbservice.result.controller;

import org.openscience.webcase.dbservice.result.model.ResultRecord;
import org.openscience.webcase.dbservice.result.service.ResultServiceImplementation;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/results")
public class ResultController {

    private final ResultServiceImplementation resultServiceImplementation;

    public ResultController(final ResultServiceImplementation resultServiceImplementation) {
        this.resultServiceImplementation = resultServiceImplementation;
    }

    @GetMapping(value = "/getById", produces = "application/json")
    public Optional<ResultRecord> getById(@RequestParam final String id) {
        return this.resultServiceImplementation.findById(id);
    }

    @GetMapping(value = "/getAll", produces = "application/json")
    public List<ResultRecord> getResultCollection() {
        return this.resultServiceImplementation.findAll();
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public String insert(@RequestBody final ResultRecord resultRecord) {
        return this.resultServiceImplementation.insert(resultRecord);
    }
}
