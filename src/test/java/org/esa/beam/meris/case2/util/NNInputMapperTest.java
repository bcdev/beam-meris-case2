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


import org.esa.beam.util.io.FileUtils;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class NNInputMapperTest {

    @Test
    public void testNoReflBands() throws Exception {
        NNInputMapper mapper = NNInputMapper.create("the net has 10 inputs:\n" +
                                                            "input  1 is sun_thet in [0.002010,75.000000]\n" +
                                                            "input  2 is view_zeni in [0.000000,50.000000]");
        String[] inputNames = mapper.getInputNames();
        assertNotNull(inputNames);
        assertEquals(0, inputNames.length);
    }

    @Test
    public void testLog() throws Exception {
        InputStream is = getClass().getResourceAsStream("/org/esa/beam/meris/case2/water_invers/23x7x16_34286.9.net");
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        String netString = FileUtils.readText(inputStreamReader);
        NNInputMapper mapper = NNInputMapper.create(netString);

        String[] inputNames = mapper.getInputNames();
        assertNotNull(inputNames);
        assertEquals(11, inputNames.length);
        assertEquals("log_rlw_412", inputNames[0]);
        assertEquals("log_rlw_443", inputNames[1]);
        assertEquals("log_rlw_490", inputNames[2]);
        assertEquals("log_rlw_510", inputNames[3]);
        assertEquals("log_rlw_560", inputNames[4]);
        assertEquals("log_rlw_620", inputNames[5]);
        assertEquals("log_rlw_665", inputNames[6]);
        assertEquals("log_rlw_708", inputNames[7]);
        assertEquals("log_rlw_753", inputNames[8]);
        assertEquals("log_rlw_778", inputNames[9]);
        assertEquals("log_rlw_865", inputNames[10]);

        int[] mapping = mapper.getMapping();
        assertNotNull(mapping);
        assertEquals(11, mapping.length);

        assertEquals(0, mapping[0]);
        assertEquals(1, mapping[1]);
        assertEquals(2, mapping[2]);
        assertEquals(3, mapping[3]);
        assertEquals(4, mapping[4]);
        assertEquals(5, mapping[5]);
        assertEquals(6, mapping[6]);

        assertEquals(8, mapping[7]);
        assertEquals(9, mapping[8]);
        assertEquals(10, mapping[9]);
        assertEquals(11, mapping[10]);

        assertTrue(mapper.isLogScaled());
    }

    @Test
    public void testWithoutLog() throws Exception {
        InputStream is = getClass().getResourceAsStream("/org/esa/beam/meris/case2/all_m1-m9/inv_iop_meris_b10/27x41x27_36447.3.net");
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        String netString = FileUtils.readText(inputStreamReader);
        NNInputMapper mapper = NNInputMapper.create(netString);

        String[] inputNames = mapper.getInputNames();
        assertNotNull(inputNames);
        assertEquals(10, inputNames.length);
        assertEquals("rw_412", inputNames[0]);
        assertEquals("rw_443", inputNames[1]);
        assertEquals("rw_489", inputNames[2]);
        assertEquals("rw_510", inputNames[3]);
        assertEquals("rw_560", inputNames[4]);
        assertEquals("rw_620", inputNames[5]);
        assertEquals("rw_665", inputNames[6]);
        assertEquals("rw_681", inputNames[7]);
        assertEquals("rw_709", inputNames[8]);
        assertEquals("rw_754", inputNames[9]);

        int[] mapping = mapper.getMapping();
        assertNotNull(mapping);
        assertEquals(10, mapping.length);

        assertEquals(0, mapping[0]);
        assertEquals(1, mapping[1]);
        assertEquals(2, mapping[2]);
        assertEquals(3, mapping[3]);
        assertEquals(4, mapping[4]);
        assertEquals(5, mapping[5]);
        assertEquals(6, mapping[6]);
        assertEquals(7, mapping[7]);
        assertEquals(8, mapping[8]);
        assertEquals(9, mapping[9]);

        assertFalse(mapper.isLogScaled());
    }


}
