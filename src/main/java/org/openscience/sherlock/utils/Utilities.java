package org.openscience.sherlock.utils;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.utils.Utils;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.MDLV3000Writer;
import org.openscience.sherlock.dbservice.dataset.controller.DataSetController;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import reactor.core.publisher.Flux;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class Utilities {

    public static Flux<DataSetRecord> getDataSetRecordFlux(
            final DataSetController dataSetController,
            final Spectrum querySpectrum, final String mf) {

        // @TODO take the nuclei order into account when matching -> now it's just an
        // exact array match
        if (mf != null) {
            return dataSetController.getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(querySpectrum
                    .getNuclei(),
                    querySpectrum.getSignalCount(),
                    Utils.getAlphabeticMF(mf));

        }

        return dataSetController.getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(querySpectrum
                .getNuclei(),
                querySpectrum.getSignalCount());
    }

    public static DataSet addMolFileToDataSet(final DataSet dataSet) throws CDKException {
        // store as MOL file
        final MDLV3000Writer mdlv3000Writer = new MDLV3000Writer();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mdlv3000Writer.setWriter(byteArrayOutputStream);
        mdlv3000Writer.write(dataSet.getStructure()
                .toAtomContainer());
        dataSet.addMetaInfo("molfile", byteArrayOutputStream.toString());

        return dataSet;
    }

    public static List<DataSet> addMolFileToDataSets(final List<DataSet> dataSetList) throws CDKException {
        for (final DataSet dataSet : dataSetList) {
            addMolFileToDataSet(dataSet);
        }

        return dataSetList;
    }
}
