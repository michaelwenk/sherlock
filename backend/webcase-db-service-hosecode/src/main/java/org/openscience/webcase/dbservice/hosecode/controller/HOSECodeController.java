package org.openscience.webcase.dbservice.hosecode.controller;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import org.openscience.webcase.dbservice.hosecode.service.HOSECodeServiceImplementation;
import org.openscience.webcase.dbservice.hosecode.service.model.HOSECode;
import org.openscience.webcase.dbservice.hosecode.service.model.HOSECodeRecord;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class HOSECodeController {

    private final String pathToNMRShiftDB = "/data/nmrshiftdb/nmrshiftdb2withsignals.sd";
    private final HOSECodeServiceImplementation hoseCodeServiceImplementation;

    public HOSECodeController(final HOSECodeServiceImplementation hoseCodeServiceImplementation) {
        this.hoseCodeServiceImplementation = hoseCodeServiceImplementation;
    }

    @GetMapping(value = "/getByID")
    public Mono<HOSECode> getByID(@RequestParam final String id) {
        return this.hoseCodeServiceImplementation.findById(id);
    }

    @GetMapping(value = "/getByHOSECode", produces = "application/json")
    public Mono<HOSECode> getByHOSECode(@RequestParam final String HOSECode) {
        return this.hoseCodeServiceImplementation.findByHoseCodeObjectHOSECode(HOSECode);
    }

    @GetMapping(value = "/count")
    public Mono<Long> getCount() {
        return this.hoseCodeServiceImplementation.count();
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<HOSECode> getAll() {
        return this.hoseCodeServiceImplementation.findAll();
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public void insert(@RequestBody final HOSECodeRecord hoseCodeRecord) {
        this.hoseCodeServiceImplementation.insert(hoseCodeRecord)
                                          .block();
    }

    @DeleteMapping(value = "/deleteAll")
    public void deleteAll() {
        this.hoseCodeServiceImplementation.deleteAll()
                                          .block();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei, final int maxSphere) {
        this.deleteAll();

        final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                new String[]{this.pathToNMRShiftDB}, new String[]{}, nuclei, maxSphere, false);
        hoseCodeShiftStatistics.keySet()
                               .forEach(hoseCode -> this.insert(new HOSECodeRecord(hoseCode, new HOSECode(hoseCode,
                                                                                                          hoseCodeShiftStatistics.get(
                                                                                                                  hoseCode)))));
    }
}
