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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps the input ogf a neural network.
 */
public class NNInputMapper {

    private static final Map<String, Integer> MERIS_WL_MAP = new HashMap<String, Integer>();

    static {
        MERIS_WL_MAP.put("412", 0);  //bb1
        MERIS_WL_MAP.put("443", 1);  //bb2
        MERIS_WL_MAP.put("489", 2);  //bb3
        MERIS_WL_MAP.put("490", 2);  //bb3
        MERIS_WL_MAP.put("510", 3);  //bb4
        MERIS_WL_MAP.put("560", 4);  //bb5
        MERIS_WL_MAP.put("620", 5);  //bb6
        MERIS_WL_MAP.put("665", 6);  //bb7
        MERIS_WL_MAP.put("681", 7);  //bb8
        MERIS_WL_MAP.put("708", 8);  //bb9
        MERIS_WL_MAP.put("709", 8);  //bb9
        MERIS_WL_MAP.put("753", 9);  //bb10
        MERIS_WL_MAP.put("754", 9);  //bb10
       // MERIS_WL_MAP.put("762", 10); //bb11
        MERIS_WL_MAP.put("778", 10); //bb12
        MERIS_WL_MAP.put("779", 10); //bb12
        MERIS_WL_MAP.put("865", 11); //bb13
       // MERIS_WL_MAP.put("885", 13); //bb14
       // MERIS_WL_MAP.put("900", 14); //bb15

       // 11, 14 + 15 are not used till now
    }

    private final String[] inputNames;
    private final int[] mapping;
    private final boolean isLogScaled;

    public NNInputMapper(String[] inputNames) throws IOException {
        this.inputNames = inputNames;
        this.mapping = new int[inputNames.length];
        Arrays.fill(mapping, -1);
        boolean log = false;
        Set<Map.Entry<String, Integer>> entries = MERIS_WL_MAP.entrySet();
        for (int i = 0; i < inputNames.length; i++) {
            String inputName = inputNames[i];
            for (Map.Entry<String, Integer> entry : entries) {
                if (inputName.endsWith(entry.getKey())) {
                    mapping[i] = entry.getValue();
                    if (inputName.startsWith("log_")) {
                        log = true;
                    }
                    break;
                }
            }
            if (mapping[i] == -1 ) {
                throw new IllegalArgumentException("Could not map NN input: " + inputName);
            }
        }
        isLogScaled = log;
    }

    public String[] getInputNames() {
        return inputNames;
    }

    public int getNumInputs() {
        return inputNames.length;
    }

    public int[] getMapping() {
        return mapping;
    }

    public boolean isLogScaled() {
        return isLogScaled;
    }

    public static NNInputMapper create(String neuralNet) throws IOException {
        Map<Integer, String> inputs = parseInputs(neuralNet);
        String[] inputNames = new String[inputs.size()];
        for (int i = 0; i < inputNames.length; i++) {
            String name = inputs.get((i + 6));
            if (name != null) {
                inputNames[i] = name;
            }
        }
        return new NNInputMapper(inputNames);
    }

    private static Map<Integer, String> parseInputs(String neuralNet) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(neuralNet));
        try {
            String line = null;
            Map<Integer, String> inputs = new HashMap<Integer, String>();
            Pattern inputPattern = Pattern.compile("input\\s+(\\d+) is (\\S+\\d\\d\\d) in");
            int maxIndex = 0;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("input")) {
                    Matcher m = inputPattern.matcher(line);
                    if (m.find()) {
                        int index = Integer.parseInt(m.group(1));
                        maxIndex = Math.max(maxIndex, index);
                        String group = m.group(2);
                        inputs.put(index, group);
                    }
                }
            }
            return inputs;
        } finally {
            bufferedReader.close();
        }
    }
}
