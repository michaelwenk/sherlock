package org.openscience.sherlock.dbservice.result.controller;

import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.nmrium.Correlations;
import com.google.gson.Gson;
import org.bson.types.ObjectId;
import org.openscience.sherlock.dbservice.result.model.db.ResultRecord;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsOperations;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(value = "/")
public class ResultController {

    private final Gson gson = new Gson();
    private final ReactiveGridFsTemplate reactiveGridFsTemplate;
    private final ReactiveGridFsOperations reactiveGridFsOperations;

    public ResultController(final ReactiveGridFsTemplate reactiveGridFsTemplate,
                            final ReactiveGridFsOperations reactiveGridFsOperations) {
        this.reactiveGridFsTemplate = reactiveGridFsTemplate;
        this.reactiveGridFsOperations = reactiveGridFsOperations;
    }

    @GetMapping(value = "/count")
    public Mono<Long> count() {
        return this.buildResultRecordFlux(new Query())
                   .count();
    }

    @GetMapping(value = "/getById", produces = "application/json")
    public Mono<ResultRecord> getById(@RequestParam final String id) {
        return this.buildResultRecordFlux(new Query(Criteria.where("_id")
                                                            .is(id)))
                   .next();
    }

    @GetMapping(value = "/getAll", produces = "application/json")
    public Flux<ResultRecord> getAll() {
        return this.buildResultRecordFlux(new Query());
    }

    @GetMapping(value = "/getAllMeta")
    public Flux<ResultRecord> getAllMeta() {
        return this.buildResultRecordFlux(new Query())
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
    public Mono<ObjectId> insert(@RequestBody final ResultRecord resultRecord) {
        return this.reactiveGridFsTemplate.store(this.resultRecordToDataBufferFlux(resultRecord), UUID.randomUUID()
                                                                                                      .toString());
    }

    @DeleteMapping(value = "/deleteById")
    public Mono<Void> deleteById(@RequestParam final String id) {
        return this.reactiveGridFsTemplate.delete(new Query(Criteria.where("_id")
                                                                    .is(id)));
    }

    @DeleteMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.reactiveGridFsTemplate.delete(new Query());
    }

    private Flux<DataBuffer> resultRecordToDataBufferFlux(final ResultRecord resultRecord) {
        final String resultString = this.gson.toJson(resultRecord, ResultRecord.class);
        final InputStream inputStream = new ByteArrayInputStream(resultString.getBytes(StandardCharsets.UTF_8));

        return DataBufferUtils.readByteChannel(() -> Channels.newChannel(inputStream),
                                               DefaultDataBufferFactory.sharedInstance, 4096);
    }

    private ResultRecord inputStreamToResultRecord(final InputStream inputStream) {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.writeBytes(inputStream.readAllBytes());
            final String resultString = byteArrayOutputStream.toString(StandardCharsets.UTF_8);

            return this.gson.fromJson(resultString, ResultRecord.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Flux<ResultRecord> buildResultRecordFlux(final Query query) {
        return this.reactiveGridFsTemplate.find(query)
                                          .flatMap(gridFSFile -> this.reactiveGridFsOperations.getResource(gridFSFile)
                                                                                              .map(reactiveGridFsResource -> {
                                                                                                  final Mono<ResultRecord> resultRecordMono = reactiveGridFsResource.getInputStream()
                                                                                                                                                                    .mapNotNull(
                                                                                                                                                                            this::inputStreamToResultRecord);
                                                                                                  return resultRecordMono.map(
                                                                                                          resultRecord -> {
                                                                                                              resultRecord.setId(
                                                                                                                      Objects.requireNonNull(
                                                                                                                                     reactiveGridFsResource.getFileId())
                                                                                                                             .toString());
                                                                                                              return resultRecord;
                                                                                                          });
                                                                                              }))
                                          .flatMap(Mono::flux);
    }
}
