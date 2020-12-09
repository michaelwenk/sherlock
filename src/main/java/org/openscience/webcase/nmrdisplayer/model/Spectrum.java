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

package org.openscience.webcase.nmrdisplayer.model;

import casekit.nmr.model.Signal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;

@NoArgsConstructor
@Getter
@Setter
@ToString

@JsonIgnoreProperties(ignoreUnknown = true)
public class Spectrum {

    private String id;
    private Default<Range> ranges;
    private Default<Zone> zones;
    private HashMap<String, Object> info;


    public casekit.nmr.model.Spectrum toSpectrum(final boolean considerSignalKind) {
        final int dimension = (int) info.get("dimension");
        final boolean isFid = (boolean) info.get("isFid");

        if (!isFid) {
            if (dimension == 1) {
                final String nucleus = (String) info.get("nucleus");
                final casekit.nmr.model.Spectrum spectrum = new casekit.nmr.model.Spectrum(new String[]{nucleus});
                ranges.getValues().forEach(range -> range.getSignal().forEach(signal1D -> {
                    if (considerSignalKind && signal1D.getKind().equals("signal")) {
                        spectrum.addSignal(new Signal(new String[]{nucleus}, new Double[]{signal1D.getDelta()}, signal1D.getMultiplicity(), signal1D.getKind(), null, 0));
                    }
                }));
                spectrum.setSolvent((String) info.get("solvent"));
                spectrum.setSpecType((String) info.get("experiment"));

                return spectrum;

            } else if (dimension == 2) {
                final String[] nuclei = ((ArrayList<String>) info.get("nucleus")).toArray(new String[]{});
                final casekit.nmr.model.Spectrum spectrum = new casekit.nmr.model.Spectrum(nuclei);

                zones.getValues().forEach(zone -> zone.getSignal().forEach(signal2D -> {
                    if (considerSignalKind && signal2D.getKind().equals("signal")) {
                        spectrum.addSignal(new Signal(nuclei, new Double[]{(Double) signal2D.getX().get("delta"), (Double) signal2D.getY().get("delta")}, signal2D.getMultiplicity(), signal2D.getKind(), null, 0));
                    }
                }));
                spectrum.setSolvent((String) info.get("solvent"));
                spectrum.setSpecType((String) info.get("experiment"));

                return spectrum;
            }
        }

        return null;
    }

}
