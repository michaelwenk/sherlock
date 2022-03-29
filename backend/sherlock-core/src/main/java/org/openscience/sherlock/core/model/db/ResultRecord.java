/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Wenk (https://github.com/michaelwenk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openscience.sherlock.core.model.db;

import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.SpectrumCompact;
import casekit.nmr.model.nmrium.Correlations;
import lombok.Data;
import org.openscience.sherlock.core.model.DetectionOptions;
import org.openscience.sherlock.core.model.ElucidationOptions;

import java.util.List;

@Data
public class ResultRecord {

    private String id;
    private String name;
    private String date;
    private List<DataSet> dataSetList;
    private DataSet previewDataSet;
    private int dataSetListSize;
    private Correlations correlations;
    private Detections detections;
    private DetectionOptions detectionOptions;
    private ElucidationOptions elucidationOptions;
    private Grouping grouping;
    private SpectrumCompact querySpectrum;
}
