package org.openscience.sherlock.dbservice.dataset.controller;

import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.controller.ConnectivityController;
import org.openscience.sherlock.dbservice.statistics.controller.HOSECodeController;
import org.openscience.sherlock.dbservice.statistics.controller.HeavyAtomStatisticsController;
import org.openscience.sherlock.dbservice.statistics.controller.HybridizationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import casekit.nmr.model.DataSet;
import reactor.core.publisher.Flux;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping(value = "/database")
public class DatabaseController {

    @Autowired
    private DataSetController dataSetController;
    @Autowired
    private ConnectivityController connectivityController;
    @Autowired
    private HybridizationController hybridizationController;
    @Autowired
    private HeavyAtomStatisticsController heavyAtomStatisticsController;
    @Autowired
    private HOSECodeController hoseCodeController;
    @Autowired
    private FragmentController fragmentController;

    @PostMapping("/fillDatabases")
    public void fillDatabases(@RequestParam String nucleus,
            @RequestParam int maxSphere) {
        System.out.println("-> building statistics: hybridization, connectivity, heavy atom statistics, hosecode...");

        final Flux<DataSet> dataSets = this.dataSetController.getByDataSetSpectrumNuclei(new String[] { nucleus })
                .map(DataSetRecord::getDataSet);
        this.connectivityController.replaceAll(dataSets);
        this.hybridizationController.replaceAll(dataSets);
        this.heavyAtomStatisticsController.replaceAll(dataSets);
        this.hoseCodeController.replaceAll(dataSets, maxSphere, true);
        this.fragmentController.replaceAll(dataSets, nucleus);

        System.out.println("-> database filling process invoked...");
    }

}
