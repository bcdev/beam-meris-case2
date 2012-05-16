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
    public void testCreate() throws Exception {
        NNInputMapper mapper = NNInputMapper.create("the net has 10 inputs:\n" +
                                                                   "input  1 is sun_thet in [0.002010,75.000000]\n" +
                                                                   "input  2 is view_zeni in [0.000000,50.000000]");
        String[] inputNames = mapper.getInputNames();
        assertNotNull(inputNames);
        assertEquals(2, inputNames.length);
        assertEquals("sun_thet", inputNames[0]);
        assertEquals("view_zeni", inputNames[1]);
    }

    @Test
    public void testCreateFromNet() throws Exception {
        InputStream is = getClass().getResourceAsStream("/org/esa/beam/meris/case2/water_invers/23x7x16_34286.9.net");
        InputStreamReader inputStreamReader = new InputStreamReader(is);
        String netString = FileUtils.readText(inputStreamReader);
        NNInputMapper mapper = NNInputMapper.create(netString);

        String[] inputNames = mapper.getInputNames();
        assertNotNull(inputNames);
        assertEquals(16, inputNames.length);
        assertEquals("sun_thet", inputNames[0]);
        assertEquals("view_zeni", inputNames[1]);
        assertEquals("azi_diff_hl", inputNames[2]);
        assertEquals("temperature", inputNames[3]);
        assertEquals("salinity", inputNames[4]);
        assertEquals("log_rlw_412", inputNames[5]);
        assertEquals("log_rlw_443", inputNames[6]);
        assertEquals("log_rlw_490", inputNames[7]);
        assertEquals("log_rlw_510", inputNames[8]);
        assertEquals("log_rlw_560", inputNames[9]);
        assertEquals("log_rlw_620", inputNames[10]);
        assertEquals("log_rlw_665", inputNames[11]);
        assertEquals("log_rlw_708", inputNames[12]);
        assertEquals("log_rlw_753", inputNames[13]);
        assertEquals("log_rlw_778", inputNames[14]);
        assertEquals("log_rlw_865", inputNames[15]);
    }

    @Test
    public void testIsLogScaled() throws Exception {
        assertTrue(NNInputMapper.isLogScaled("log_rlw_865"));
        assertFalse(NNInputMapper.isLogScaled("rlw_865"));
    }

    @Test
    public void testGetReflBandIndex() throws Exception {
        NNInputMapper.getReflBandIndex("log_rlw_412");
    }

}
