package org.openscience.webcase.dbservice.result.controller;

import org.openscience.webcase.dbservice.result.model.db.ResultRecord;
import org.openscience.webcase.dbservice.result.service.ResultServiceImplementation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/")
public class ResultController {

    private final ResultServiceImplementation resultServiceImplementation;

    public ResultController(final ResultServiceImplementation resultServiceImplementation) {
        this.resultServiceImplementation = resultServiceImplementation;
    }

    @GetMapping(value = "/getById", produces = "application/json")
    public Mono<ResultRecord> getById(@RequestParam final String id) {
        return this.resultServiceImplementation.findById(id);
    }

    @PostMapping(value = "/insert", consumes = "application/json", produces = "application/json")
    public Mono<ResultRecord> insert(@RequestBody final ResultRecord resultRecord) {
        return this.resultServiceImplementation.insert(resultRecord);
    }
}
