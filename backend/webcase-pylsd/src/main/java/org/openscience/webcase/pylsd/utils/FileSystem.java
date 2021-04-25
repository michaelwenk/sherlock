/*
 * The MIT License
 *
 * Copyright 2019 Michael Wenk [https://github.com/michaelwenk]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.openscience.webcase.pylsd.utils;

import org.openscience.webcase.pylsd.model.DataSet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSystem {

    public static BufferedReader readFile(final String pathToFile) {
        try {
            return new BufferedReader(new FileReader(pathToFile));
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean writeFile(final String pathToFile, final String content) {
        try {
            final FileWriter fileWriter = new FileWriter(pathToFile);
            final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.close();

            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static List<String> getSmilesListFromFile(final String pathToSmilesFile) {
        final List<String> smilesList = new ArrayList<>();
        try {
            final BufferedReader bufferedReader = FileSystem.readFile(pathToSmilesFile);
            if (bufferedReader
                    != null) {
                String line;
                while ((line = bufferedReader.readLine())
                        != null) {
                    smilesList.add(line);
                }
                bufferedReader.close();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return smilesList;
    }

    public static boolean cleanup(final String[] directoriesToCheck, final String requestID) {
        boolean cleaned = false;

        for (final String dir : directoriesToCheck) {
            try {
                cleaned = Files.walk(Paths.get(dir))
                               .map(Path::toFile)
                               .filter(file -> file.getAbsolutePath()
                                                   .contains(requestID))
                               .allMatch(File::delete);

            } catch (final IOException e) {
                System.out.println("Not all files could be deleted!");
                e.printStackTrace();
            }
        }

        return cleaned;
    }

    public static List<DataSet> retrieveResultFromSmilesFile(final String pathToResultsFile) {
        final List<DataSet> dataSetList = new ArrayList<>();
        final List<String> smilesList = FileSystem.getSmilesListFromFile(pathToResultsFile);

        DataSet dataSet;
        Map<String, String> meta;
        for (final String smiles : smilesList) {
            meta = new HashMap<>();
            meta.put("smiles", smiles);
            dataSet = new DataSet();
            dataSet.setMeta(meta);

            dataSetList.add(dataSet);
        }

        return dataSetList;
    }
}
