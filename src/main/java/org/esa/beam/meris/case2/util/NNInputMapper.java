/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.meris.case2.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps the input ogf a neural network.
 */
public class NNInputMapper {

    private static final int[] MERIS_WL = new int[]{
            412, // 1
            443, // 2
            489, // 3
            510, // 4
            560, // 5
            620, // 6
            665, // 7
            681, // 8
            708, // 9
            754, //10
            762, //11 check unused
            778, //12 check unused
            865, //13 check unused
            885, //14 check unused
            900  //15 check unused
    };

    private final String[] inputNames;

    public NNInputMapper(String[] inputNames) throws IOException {
        this.inputNames = inputNames;
    }

    public String[] getInputNames() {
        return inputNames;
    }

    static int getReflBandIndex(String nnVarName) {
        return 0;
    }

    static NNInputMapper create(String neuralNet) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(neuralNet));
        String line = null;
        Map<Integer, String> inputs = new HashMap<Integer, String> ();
        Pattern inputPattern = Pattern.compile("input\\s+(\\d+) is (\\S+)");
        int maxIndex = 0;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("input")) {
                Matcher m = inputPattern.matcher(line);
                if (m.find()) {
                    int index = Integer.parseInt(m.group(1));
                    maxIndex = Math.max(maxIndex, index);
                    inputs.put(index, m.group(2));
                }
            }
        }
        String[] inputNames = new String[maxIndex];
        for (int i = 0; i < inputNames.length; i++) {
            String name = inputs.get((i + 1));
            if (name != null) {
                inputNames[i] = name;
            }
        }
        return new NNInputMapper(inputNames);
    }


    public static boolean isLogScaled(String name) {
        return name.startsWith("log_");
    }
}
