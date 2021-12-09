/*
 * MIT License
 *
 * Copyright (c) 2021 Michael Wenk (https://github.com/michaelwenk)
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

package org.openscience.sherlock.core.model.exchange;

import casekit.nmr.lsd.model.Detections;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.openscience.sherlock.core.model.DereplicationOptions;
import org.openscience.sherlock.core.model.DetectionOptions;
import org.openscience.sherlock.core.model.ElucidationOptions;
import org.openscience.sherlock.core.model.db.ResultRecord;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {
    private List<DataSet> dataSetList;
    private Data data;
    private Spectrum querySpectrum;
    private String mf;
    private String queryType;
    private DereplicationOptions dereplicationOptions;
    private ElucidationOptions elucidationOptions;
    private DetectionOptions detectionOptions;
    private Detections detections;
    private String requestID;
    private ResultRecord resultRecord;
    // internal only
    private Boolean pyLSDRunWasSuccessful;
    // error message
    private String errorMessage;
}
