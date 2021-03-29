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

package org.openscience.webcase.dbservice.result.service;

import org.openscience.webcase.dbservice.result.model.DataSet;
import org.openscience.webcase.dbservice.result.model.db.ResultRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class ResultServiceImplementation
        implements ResultService {

    private final ResultRepository resultRepository;

    @Autowired
    public ResultServiceImplementation(final ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Override
    public long count() {
        return this.resultRepository.count();
    }

    @Override
    public String insert(final List<DataSet> dataSetList) {
        final ResultRecord resultRecord = new ResultRecord();
        resultRecord.setDataSetList(dataSetList);

        return this.resultRepository.insert(resultRecord)
                                    .getId();
    }

    @Override
    public List<DataSet> findById(final String id) {
        List<DataSet> dataSetList = new ArrayList<>();
        final Optional<ResultRecord> resultRecordOptional = this.resultRepository.findById(id);
        if (resultRecordOptional.isPresent()) {
            dataSetList = resultRecordOptional.get()
                                              .getDataSetList();
        }

        return dataSetList;
    }

    @Override
    public void deleteAll() {
        this.resultRepository.deleteAll();
    }
}
