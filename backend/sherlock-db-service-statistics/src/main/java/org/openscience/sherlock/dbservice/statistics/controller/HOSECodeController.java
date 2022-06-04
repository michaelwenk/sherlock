package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Statistics;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeRepositoryReactive;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(value = "/hosecode")
public class HOSECodeController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final HOSECodeServiceImplementation hoseCodeServiceImplementation;
    private final HOSECodeRepositoryReactive hoseCodeRepositoryReactive;

    @Autowired
    public HOSECodeController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies,
                              final HOSECodeServiceImplementation hoseCodeServiceImplementation,
                              final HOSECodeRepositoryReactive hoseCodeRepositoryReactive) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.hoseCodeServiceImplementation = hoseCodeServiceImplementation;
        this.hoseCodeRepositoryReactive = hoseCodeRepositoryReactive;
    }

    private String decode(final String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    @GetMapping(value = "/getByID")
    public Optional<HOSECodeRecord> getByID(@RequestParam final String id) {
        return this.hoseCodeServiceImplementation.findById(this.decode(id));
    }

    @GetMapping(value = "/count")
    public long getCount() {
        return this.hoseCodeServiceImplementation.count();
    }

    @GetMapping(value = "/getAll")
    public List<HOSECodeRecord> getAll() {
        return this.hoseCodeServiceImplementation.findAll();
    }


    @DeleteMapping(value = "/deleteAll")
    public void deleteAll() {
        this.hoseCodeServiceImplementation.deleteAll();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei, @RequestParam final int maxSphere) {
        System.out.println(" --> delete all DB entries...");
        this.deleteAll();
        System.out.println(" --> deleted all DB entries!");

        System.out.println(" --> fetching all datasets, build HOSE code collection and store...");
        Utilities.getByDataSetSpectrumNuclei(this.webClientBuilder, this.exchangeStrategies, nuclei)
                 .doOnNext(dataSetRecord -> {
                     final List<DataSet> dataSetList = new ArrayList<>();
                     dataSetList.add(dataSetRecord.getDataSet());
                     final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                             dataSetList, maxSphere, true, false);
                     for (final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode : hoseCodeShiftStatisticsTemp.entrySet()) {
                         final String hoseCode = entryPerHOSECode.getKey();
                         if (!this.hoseCodeServiceImplementation.existsById(hoseCode)) {
                             final HOSECodeRecord hoseCodeRecord = new HOSECodeRecord(hoseCode, new HashMap<>(),
                                                                                      new HashMap<>());
                             this.insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                             this.hoseCodeServiceImplementation.insert(hoseCodeRecord);
                         } else {
                             final HOSECodeRecord hoseCodeRecord = this.hoseCodeServiceImplementation.findById(hoseCode)
                                                                                                     .get();
                             this.insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                             this.hoseCodeServiceImplementation.save(hoseCodeRecord);
                         }
                     }
                 })
                 .doAfterTerminate(() -> {
                     System.out.println(" -> build and store HOSE code collection done: "
                                                + this.getCount());
                 })
                 .doOnError(Throwable::printStackTrace)
                 .subscribe();
    }

    private void insertIntoHoseCodeRecord(final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode,
                                          final HOSECodeRecord hoseCodeRecord) {
        String solvent, shiftString;
        for (final Map.Entry<String, Double[]> entryPerSolvent : entryPerHOSECode.getValue()
                                                                                 .entrySet()) {
            solvent = entryPerSolvent.getKey();
            hoseCodeRecord.getValues()
                          .putIfAbsent(solvent, new HashMap<>());
            for (final Double shift : entryPerSolvent.getValue()) {
                shiftString = String.valueOf(Statistics.roundDouble(shift, 1))
                                    .replaceAll("\\.", "_");
                hoseCodeRecord.getValues()
                              .get(solvent)
                              .putIfAbsent(shiftString, 0L);
                hoseCodeRecord.getValues()
                              .get(solvent)
                              .put(shiftString, hoseCodeRecord.getValues()
                                                              .get(solvent)
                                                              .get(shiftString)
                                      + 1);
            }
        }
    }

    @PostMapping(value = "/buildStatistics")
    public void buildStatistics() {
        final AtomicInteger count = new AtomicInteger(0);
        this.hoseCodeRepositoryReactive.findAll()
                                       .doOnNext(hoseCodeRecord -> {
                                           final Map<String, Double[]> statistics = new HashMap<>();
                                           List<Double> values;
                                           double shift;

                                           for (final Map.Entry<String, Map<String, Long>> entryPerSolvent : hoseCodeRecord.getValues()
                                                                                                                           .entrySet()) {
                                               values = new ArrayList<>();
                                               for (final Map.Entry<String, Long> entryPerShiftString : entryPerSolvent.getValue()
                                                                                                                       .entrySet()) {
                                                   shift = Double.parseDouble(entryPerShiftString.getKey()
                                                                                                 .replaceAll("_",
                                                                                                             "\\."));
                                                   if (shift
                                                           == 1.0) {
                                                       continue;
                                                   }
                                                   for (long i = 0; i
                                                           < entryPerShiftString.getValue(); i++) {
                                                       values.add(shift);
                                                   }
                                               }
                                               if (!values.isEmpty()) {
                                                   statistics.put(entryPerSolvent.getKey(),
                                                                  new Double[]{(double) values.size(),
                                                                               Collections.min(values),
                                                                               Statistics.getMean(values),
                                                                               Statistics.getMedian(values),
                                                                               Collections.max(values)});
                                               }
                                           }
                                           hoseCodeRecord.setStatistics(statistics);
                                           this.hoseCodeRepositoryReactive.save(hoseCodeRecord)
                                                                          .subscribe();

                                           if (count.incrementAndGet()
                                                   % 100000
                                                   == 0) {
                                               System.out.println(" -> reached: "
                                                                          + count.get());
                                           }
                                       })
                                       .doAfterTerminate(() -> {
                                           System.out.println(" -> build statistics done");
                                       })
                                       .subscribe();

    }

    //    @GetMapping(value = "/saveAllAsMap")
    //    public void saveAllAsMap() {
    //
    //        final Gson gson = new GsonBuilder().create();
    //        final String pathToHOSECodesFile = "/data/hosecode/hosecodes.json";
    //        System.out.println("-> store json file in shared volume under \""
    //                                   + pathToHOSECodesFile
    //                                   + "\"");
    //
    //        final StringBuilder stringBuilder = new StringBuilder();
    //        stringBuilder.append("[");
    //
    //        this.getAll()
    //            .doOnNext(hoseCodeObject -> stringBuilder.append(gson.toJson(hoseCodeObject))
    //                                                     .append(","))
    //            .doAfterTerminate(() -> {
    //                stringBuilder.deleteCharAt(stringBuilder.length()
    //                                                   - 1);
    //                stringBuilder.append("]");
    //                FileSystem.writeFile(pathToHOSECodesFile, stringBuilder.toString());
    //                System.out.println("-> done");
    //            })
    //            .subscribe();
    //    }
}
