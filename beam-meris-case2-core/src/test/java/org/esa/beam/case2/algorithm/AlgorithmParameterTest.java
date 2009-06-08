/*
 * $Id: AlgorithmParameterTest.java,v 1.7 2006/11/06 09:37:09 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.case2.algorithm;

import junit.framework.TestCase;
import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.util.ObjectIO;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

public class AlgorithmParameterTest extends TestCase {
    public void testRead() throws IOException {
        final AlgorithmParameter parameter = read("test-params.properties");
        assertTrue(parameter.performChiSquareFit);
        assertEquals("C:\\auxdata\\water-inv.net", parameter.waterNnInverseFilePath);
        assertEquals("C:\\auxdata\\water-forw.net", parameter.waterNnForwardFilePath);
        assertEquals("C:\\auxdata\\atm-corr.net", parameter.atmCorrNnFilePath);
        assertEquals(0.34, parameter.tsmConversionFactor, 1e-10);
        assertEquals(-0.24, parameter.tsmConversionExponent, 1e-10);
        assertEquals(0.129, parameter.chlConversionFactor, 1e-10);
        assertEquals(-0.5, parameter.chlConversionExponent, 1e-10);
        assertEquals(0.61, parameter.spectrumOutOfScopeThreshold, 1e-10);
        assertEquals(0.56, parameter.radiance1AdjustmentFactor, 1e-10);


        assertTrue(parameter.outputAPig);
        assertTrue(parameter.outputAGelb);
        assertTrue(parameter.outputBTsm);
        assertFalse(parameter.outputChlConc);
        assertTrue(parameter.outputTsmConc);
        assertFalse(parameter.outputOutOfScopeChiSquare);

        testArray("outputPathRadianceRefl", new boolean[] {
            true, // 1
            true, // 2
            false, // 3
            false, // 4
            false, // 5
            false, // 6
            false, // 7
            false, // 8
            false, // 9
        }, parameter.outputPathRadianceRefl);

        testArray("outputWaterLeavingRefl", new boolean[] {
            false, // 1
            false, // 2
            true, // 3
            false, // 4
            false, // 5
            false, // 6
            true, // 7
            false, // 8
            false, // 9
       }, parameter.outputWaterLeavingRefl);

        testArray("outputTransmittance", new boolean[] {
            true, // 1
            false, // 2
            false, // 3
            true, // 4
            false, // 5
            false, // 6
            false, // 7
            true, // 8
            false, // 9
        }, parameter.outputTransmittance);

    }

    public void testParamsIfNotInParamsFile() throws IOException {
        final AlgorithmParameter parameter = read("test-params_nothing.properties");
        assertEquals("./atmo_net_20080513/25x30x35x40_5407.2.net",parameter.atmCorrNnFilePath);
        assertEquals("toa_reflec_10 > toa_reflec_6 AND toa_reflec_13 > 0.0475", parameter.landWaterSeparationExpression);
        assertEquals("toa_reflec_14 > 0.2", parameter.cloudIceDetectionExpression);
        
    }

    private void testArray(final String name, boolean[] expected, final boolean[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(MessageFormat.format("{0}[{1}]", name, i), expected[i], actual[i]);
        }
    }

    private AlgorithmParameter read(final String propertyFileName) throws IOException {
        final InputStream stream = AlgorithmParameterTest.class.getResourceAsStream(propertyFileName);
        try {
            return ObjectIO.readObject(AlgorithmParameter.class, stream);
        } finally {
            stream.close();
        }
    }
}