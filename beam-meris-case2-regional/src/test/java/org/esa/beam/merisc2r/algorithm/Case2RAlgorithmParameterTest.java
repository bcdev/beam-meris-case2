package org.esa.beam.merisc2r.algorithm;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.esa.beam.case2.util.ObjectIO;

public class Case2RAlgorithmParameterTest extends TestCase {
    public void testRead() throws IOException {
        final Case2RAlgorithmParameter parameter = read("test-params.properties");
        assertTrue(parameter.performChiSquareFit);
        assertEquals("C:\\auxdata\\water-inv.net", parameter.waterNnInverseFilePath);
        assertEquals("C:\\auxdata\\water-forw.net", parameter.waterNnForwardFilePath);
        assertEquals("C:\\auxdata\\atm-corr.net", parameter.atmCorrNnFilePath);
        assertEquals(0.34, parameter.tsmConversionFactor, 1e-10);
        assertEquals(-0.24, parameter.tsmConversionExponent, 1e-10);
        assertEquals(0.129, parameter.chlConversionFactor, 1e-10);
        assertEquals(-0.5, parameter.chlConversionExponent, 1e-10);
        assertEquals(0.61, parameter.spectrumOutOfScopeThreshold, 1e-10);
        assertEquals(0.99, parameter.radiance1AdjustmentFactor, 1e-10);


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
        final Case2RAlgorithmParameter parameter = read("test-params_nothing.properties");
        assertEquals("./water_net_20040320/meris_bn_20040322_45x16x12x8x5_5177.9.net", parameter.waterNnInverseFilePath);
        assertEquals("./water_net_20040320/meris_fn_20040319_15x15x15_1750.4.net", parameter.waterNnForwardFilePath);
        assertEquals(1, parameter.radiance1AdjustmentFactor, 1e-10);
        assertEquals("./atmo_net_20091215/25x30x40_9164.3.net",parameter.atmCorrNnFilePath);
    }

    private void testArray(final String name, boolean[] expected, final boolean[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(MessageFormat.format("{0}[{1}]", name, i), expected[i], actual[i]);
        }
    }

    private Case2RAlgorithmParameter read(final String propertyFileName) throws IOException {
        final InputStream stream = Case2RAlgorithmParameterTest.class.getResourceAsStream(propertyFileName);
        try {
            return ObjectIO.readObject(Case2RAlgorithmParameter.class, stream);
        } finally {
            stream.close();
        }
    }


}
