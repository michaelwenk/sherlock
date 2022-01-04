package org.openscience.sherlock.dbservice.result.controller;

import casekit.nmr.lsd.model.Detections;
import casekit.nmr.lsd.model.Grouping;
import casekit.nmr.model.nmrium.Correlations;
import org.openscience.sherlock.dbservice.result.model.db.ResultRecord;
import org.openscience.sherlock.dbservice.result.service.ResultServiceImplementation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@RestController
@RequestMapping(value = "/")
public class ResultController {

    private final ResultServiceImplementation resultServiceImplementation;

    public ResultController(final ResultServiceImplementation resultServiceImplementation) {
        this.resultServiceImplementation = resultServiceImplementation;
    }

    @GetMapping(value = "/count")
    public Mono<Long> count() {
        return this.resultServiceImplementation.count();
    }

    @GetMapping(value = "/getById", produces = "application/json")
    public Mono<ResultRecord> getById(@RequestParam final String id) {
        return this.resultServiceImplementation.findById(id);
    }

    @GetMapping(value = "/getAll", produces = "application/json")
    public Flux<ResultRecord> getAll() {
        return this.resultServiceImplementation.findAll();
    }

    @GetMapping(value = "/getAllMeta")
    public Flux<ResultRecord> getAllMeta() {
        return this.resultServiceImplementation.findAll()
                                               .map(resultRecord -> {
                                                   // empty the following to save unnecessary data transfer
                                                   resultRecord.setDataSetList(new ArrayList<>());
                                                   resultRecord.setCorrelations(new Correlations());
                                                   resultRecord.setDetections(new Detections());
                                                   resultRecord.setGrouping(new Grouping());
                                                   return resultRecord;
                                               });
    }

    @PostMapping(value = "/insert", consumes = "application/json", produces = "application/json")
    public Mono<ResultRecord> insert(@RequestBody final ResultRecord resultRecord) {
        return this.resultServiceImplementation.insert(resultRecord);
    }

    @DeleteMapping(value = "/deleteById")
    public Mono<Void> deleteById(@RequestParam final String id) {
        return this.resultServiceImplementation.deleteById(id);
    }

    @DeleteMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.resultServiceImplementation.deleteAll();
    }
}
