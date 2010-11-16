package org.esa.beam.lakes.eutrophic.algorithm;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.esa.beam.case2.util.ObjectIO;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

public class EutrophicAlgorithmParameterTest extends TestCase {

    public void testRead() throws IOException {
        final EutrophicAlgorithmParameter parameter = read("test-params.properties");
        assertTrue(parameter.performChiSquareFit);
        Assert.assertEquals("C:\\auxdata\\water-inv.net", parameter.waterNnInverseFilePath);
        Assert.assertEquals("C:\\auxdata\\water-forw.net", parameter.waterNnForwardFilePath);
        Assert.assertEquals("C:\\auxdata\\atm-corr.net", parameter.atmCorrNnFilePath);
        Assert.assertEquals(0.34, parameter.tsmConversionFactor, 1.0e-10);
        Assert.assertEquals(-0.24, parameter.tsmConversionExponent, 1.0e-10);
        Assert.assertEquals(0.129, parameter.chlConversionFactor, 1.0e-10);
        Assert.assertEquals(-0.5, parameter.chlConversionExponent, 1.0e-10);
        Assert.assertEquals(0.61, parameter.spectrumOutOfScopeThreshold, 1.0e-10);
        Assert.assertEquals(0.33, parameter.radiance1AdjustmentFactor, 1.0e-10);


        assertTrue(parameter.outputAPig);
        assertTrue(parameter.outputAGelb);
        assertTrue(parameter.outputBTsm);
        assertFalse(parameter.outputChlConc);
        assertTrue(parameter.outputTsmConc);
        assertFalse(parameter.outputOutOfScopeChiSquare);

        testArray("outputPathRadianceRefl", new boolean[]{
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

        testArray("outputWaterLeavingRefl", new boolean[]{
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

        testArray("outputTransmittance", new boolean[]{
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
        final EutrophicAlgorithmParameter parameter = read("test-params_nothing.properties");
        Assert.assertEquals("./water_net_eutrophic_20070710/60x20_586.8inv.net", parameter.waterNnInverseFilePath);
        Assert.assertEquals("./water_net_eutrophic_20070710/30x15_88.8forw.net", parameter.waterNnForwardFilePath);
        Assert.assertEquals(1.0, parameter.radiance1AdjustmentFactor, 1.0e-10);
        Assert.assertEquals("./atmo_net_20091215/25x30x40_9164.3.net", parameter.atmCorrNnFilePath);

    }

    private void testArray(final String name, boolean[] expected, final boolean[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(MessageFormat.format("{0}[{1}]", name, i), expected[i], actual[i]);
        }
    }

    private EutrophicAlgorithmParameter read(final String propertyFileName) throws IOException {
        final InputStream stream = EutrophicAlgorithmParameterTest.class.getResourceAsStream(propertyFileName);
        try {
            return ObjectIO.readObject(EutrophicAlgorithmParameter.class, stream);
        } finally {
            stream.close();
        }
    }


}