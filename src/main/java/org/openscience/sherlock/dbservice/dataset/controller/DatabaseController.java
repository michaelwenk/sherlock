package org.openscience.sherlock.dbservice.dataset.controller;

import org.openscience.sherlock.dbservice.statistics.controller.ConnectivityController;
import org.openscience.sherlock.dbservice.statistics.controller.HOSECodeController;
import org.openscience.sherlock.dbservice.statistics.controller.HeavyAtomStatisticsController;
import org.openscience.sherlock.dbservice.statistics.controller.HybridizationController;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import casekit.nmr.model.DataSet;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Database Maintenance", description = "Endpoints for rebuilding Sherlock dataset-derived statistics collections.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/database")
public class DatabaseController {

    private final DataSetController dataSetController;
    private final ConnectivityController connectivityController;
    private final HybridizationController hybridizationController;
    private final HeavyAtomStatisticsController heavyAtomStatisticsController;
    private final HOSECodeController hoseCodeController;
    private final FragmentController fragmentController;

    public DatabaseController(final DataSetController dataSetController,
            final ConnectivityController connectivityController,
            final HybridizationController hybridizationController,
            final HeavyAtomStatisticsController heavyAtomStatisticsController,
            final HOSECodeController hoseCodeController,
            final FragmentController fragmentController) {
        this.dataSetController = dataSetController;
        this.connectivityController = connectivityController;
        this.hybridizationController = hybridizationController;
        this.heavyAtomStatisticsController = heavyAtomStatisticsController;
        this.hoseCodeController = hoseCodeController;
        this.fragmentController = fragmentController;
    }

    @Operation(summary = "Fill derived databases", description = "Builds connectivity, hybridization, heavy atom, HOSE code, and fragment statistics for the selected nucleus.")
    @PostMapping("/fillDatabases")
    public Mono<Void> fillDatabases(@RequestParam String nucleus,
            @RequestParam int maxSphere) {
        System.out.println("-> building statistics: hybridization, connectivity, heavy atom statistics, hosecode...");

        final Flux<DataSet> dataSets = this.dataSetController.getByDataSetSpectrumNuclei(new String[] { nucleus })
            .map(dataSetRecord -> dataSetRecord.getDataSet());
        this.connectivityController.replaceAll(dataSets);
        this.hybridizationController.replaceAll(dataSets);
        this.heavyAtomStatisticsController.replaceAll(dataSets);
        this.fragmentController.replaceAll(dataSets, nucleus);

        return this.hoseCodeController.replaceAll(dataSets, maxSphere, true)
            .doOnSubscribe(unused -> System.out.println("-> database filling process invoked..."));
    }

}
