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
    @Autowired
    private WebClient.Builder webClientBuilder;

    public HybridizationController(final HybridizationServiceImplementation hybridizationServiceImplementation) {
        this.hybridizationServiceImplementation = hybridizationServiceImplementation;
    }


    @GetMapping(value = "/count", produces = "application/json")
    public long getCount() {
        return this.hybridizationServiceImplementation.count();
    }

    @GetMapping(value = "/getAll", produces = "application/json")
    public List<HybridizationRecord> getAll() {
        return this.hybridizationServiceImplementation.findAll();
    }

    @GetMapping(value = "/detectHybridizations", produces = "application/json")
    public List<Integer> detectHybridization(@RequestParam final String nucleus, @RequestParam final int minShift,
                                             @RequestParam final int maxShift, @RequestParam final String multiplicity,
                                             @RequestParam final float thrs) {
        final List<String> hybridizations = this.hybridizationServiceImplementation.aggregateHybridizationsByNucleusAndShiftAndMultiplicity(
                nucleus, minShift, maxShift, multiplicity);
        final Set<String> uniqueLabels = new HashSet<>(hybridizations);
        final Set<Integer> uniqueValues = new HashSet<>();

        uniqueLabels.forEach(label -> {
            if (Constants.hybridizationConversionMap.containsKey(nucleus)
                    && Constants.hybridizationConversionMap.get(nucleus)
                                                           .containsKey(label)
                    && hybridizations.stream()
                                     .filter(value -> value.equals(label))
                                     .count()
                    / (float) hybridizations.size()
                    >= thrs) {
                uniqueValues.add(Constants.hybridizationConversionMap.get(nucleus)
                                                                     .get(label));
            }
        });

        return new ArrayList<>(uniqueValues);
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        this.hybridizationServiceImplementation.deleteAll();

        String atomType, nucleus, hybridization, multiplicity;
        Integer shift;
        IAtomContainer structure;
        int atomIndex;
        int[][][] assignmentValues;
        final List<DataSet> dataSetList = this.getByDataSetSpectrumNuclei(nuclei)
                                              .map(DataSetRecord::getDataSet)
                                              .collectList()
                                              .block();
        if (dataSetList
                != null) {
            for (final DataSet dataset : dataSetList) {
                System.out.println(dataset);
                System.out.println(dataset.getStructure());
                nucleus = dataset.getSpectrum()
                                 .getNuclei()[0];
                atomType = Utils.getAtomTypeFromNucleus(nucleus);
                structure = dataset.getStructure()
                                   .toAtomContainer();
                System.out.println(structure);
                assignmentValues = dataset.getAssignment()
                                          .getAssignments();
                for (int signalIndex = 0; signalIndex
                        < assignmentValues[0].length; signalIndex++) {
                    multiplicity = dataset.getSpectrum()
                                          .getSignal(signalIndex)
                                          .getMultiplicity();
                    shift = null;
                    if (dataset.getSpectrum()
                               .getSignals()
                               .get(signalIndex)
                               .getShifts()[0]
                            != null) {
                        shift = dataset.getSpectrum()
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

                        this.hybridizationServiceImplementation.insert(
                                new HybridizationRecord(null, nucleus, shift, multiplicity, hybridization));
                    }
                }
            }
        }
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
