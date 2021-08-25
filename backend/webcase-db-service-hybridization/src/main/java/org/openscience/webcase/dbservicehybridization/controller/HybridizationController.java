package org.openscience.webcase.dbservicehybridization.controller;

import casekit.nmr.lsd.Constants;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.webcase.dbservicehybridization.service.HybridizationServiceImplementation;
import org.openscience.webcase.dbservicehybridization.service.model.DataSetRecord;
import org.openscience.webcase.dbservicehybridization.service.model.HybridizationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@RestController
@RequestMapping(value = "/")
public class HybridizationController {

    // set ExchangeSettings
    final int maxInMemorySizeMB = 1000;
    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            this.maxInMemorySizeMB
                                                                                                                    * 1024

                                                                                                                    * 1024))
                                                                    .build();
    private final HybridizationServiceImplementation hybridizationServiceImplementation;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public HybridizationController(final HybridizationServiceImplementation hybridizationServiceImplementation,
                                   final WebClient.Builder webClientBuilder) {
        this.hybridizationServiceImplementation = hybridizationServiceImplementation;
        this.webClientBuilder = webClientBuilder;
    }


    @GetMapping(value = "/count", produces = "application/json")
    public Long getCount() {
        return this.hybridizationServiceImplementation.count()
                                                      .block();
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<HybridizationRecord> getAll() {
        return this.hybridizationServiceImplementation.findAll();
    }

    @GetMapping(value = "/detectHybridizations", produces = "application/json")
    public List<Integer> detectHybridization(@RequestParam final String nucleus,
                                             @RequestParam final String multiplicity, @RequestParam final int minShift,
                                             @RequestParam final int maxShift, @RequestParam final float thrs) {
        if (!Constants.hybridizationConversionMap.containsKey(nucleus)) {
            System.out.println("Unknown nucleus!!!");
            return new ArrayList<>();
        }

        final List<HybridizationRecord> hybridizationRecordList = this.hybridizationServiceImplementation.findByNucleusAndMultiplicityAndShift(
                nucleus, multiplicity, minShift, maxShift)
                                                                                                         .collectList()
                                                                                                         .block();
        final Map<String, Integer> totalHybridizationCounts = new HashMap<>();
        int totalCount = 0;
        for (final HybridizationRecord hybridizationRecord : hybridizationRecordList) {
            for (final String hybridization : hybridizationRecord.getHybridizationCounts()
                                                                 .keySet()) {
                if (Constants.hybridizationConversionMap.get(nucleus)
                                                        .containsKey(hybridization)) {
                    totalHybridizationCounts.putIfAbsent(hybridization, 0);
                    totalHybridizationCounts.put(hybridization, totalHybridizationCounts.get(hybridization)
                            + hybridizationRecord.getHybridizationCounts()
                                                 .get(hybridization));
                    totalCount += hybridizationRecord.getHybridizationCounts()
                                                     .get(hybridization);
                }
            }
        }
        final List<Integer> validHydridizations = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : totalHybridizationCounts.entrySet()) {
            if (((double) entry.getValue()
                    / totalCount)
                    >= thrs) {
                validHydridizations.add(Constants.hybridizationConversionMap.get(nucleus)
                                                                            .get(entry.getKey()));
            }
        }

        return validHydridizations;
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        this.hybridizationServiceImplementation.deleteAll()
                                               .block();

        final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<String, ConcurrentLinkedDeque<String>>>> entries = new ConcurrentHashMap<>(); // nucleus, shift, multiplicity, list of hybridizations
        this.getByDataSetSpectrumNuclei(nuclei)
            .doOnNext(dataSetRecord -> {
                final DataSet dataSet = dataSetRecord.getDataSet();
                final String nucleus = dataSet.getSpectrum()
                                              .getNuclei()[0];
                final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
                final IAtomContainer structure = dataSet.getStructure()
                                                        .toAtomContainer();
                final int[][][] assignmentValues = dataSet.getAssignment()
                                                          .getAssignments();
                String multiplicity, hybridization;
                Integer shift;
                int atomIndex;
                for (int signalIndex = 0; signalIndex
                        < assignmentValues[0].length; signalIndex++) {
                    multiplicity = dataSet.getSpectrum()
                                          .getSignal(signalIndex)
                                          .getMultiplicity();
                    if (multiplicity
                            == null) {
                        continue;
                    }
                    shift = null;
                    if (dataSet.getSpectrum()
                               .getSignals()
                               .get(signalIndex)
                               .getShifts()[0]
                            != null) {
                        shift = dataSet.getSpectrum()
                                       .getSignal(signalIndex)
                                       .getShift(0)
                                       .intValue();
                    }
                    for (int equivalenceIndex = 0; equivalenceIndex
                            < assignmentValues[0][signalIndex].length; equivalenceIndex++) {
                        atomIndex = assignmentValues[0][signalIndex][equivalenceIndex];
                        hybridization = structure.getAtom(atomIndex)
                                                 .getHybridization()
                                                 .name();
                        if (shift
                                == null
                                || structure.getAtom(atomIndex)
                                            .getSymbol()
                                == null
                                || !structure.getAtom(atomIndex)
                                             .getSymbol()
                                             .equals(atomType)) {
                            continue;
                        }
                        entries.putIfAbsent(nucleus, new ConcurrentHashMap<>());
                        entries.get(nucleus)
                               .putIfAbsent(shift, new ConcurrentHashMap<>());
                        entries.get(nucleus)
                               .get(shift)
                               .putIfAbsent(multiplicity, new ConcurrentLinkedDeque<>());
                        entries.get(nucleus)
                               .get(shift)
                               .get(multiplicity)
                               .add(hybridization);
                    }
                }
            })
            .doAfterTerminate(() -> {
                Map<String, Integer> hybridizationCounts;
                for (final String nucleus : entries.keySet()) {
                    for (final int shift : entries.get(nucleus)
                                                  .keySet()) {
                        for (final String multiplicity : entries.get(nucleus)
                                                                .get(shift)
                                                                .keySet()) {
                            hybridizationCounts = new HashMap<>();
                            for (final String hybridization : entries.get(nucleus)
                                                                     .get(shift)
                                                                     .get(multiplicity)) {
                                hybridizationCounts.putIfAbsent(hybridization, 0);
                                hybridizationCounts.put(hybridization, hybridizationCounts.get(hybridization)
                                        + 1);
                            }
                            this.hybridizationServiceImplementation.insert(
                                    new HybridizationRecord(null, nucleus, shift, multiplicity, hybridizationCounts))
                                                                   .subscribe();
                        }
                    }
                }
            })
            .subscribe();
    }

    public Flux<DataSetRecord> getByDataSetSpectrumNuclei(final String[] nuclei) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-dataset")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(nuclei)
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getByNuclei")
                            .queryParam("nuclei", nucleiString);

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }
}
