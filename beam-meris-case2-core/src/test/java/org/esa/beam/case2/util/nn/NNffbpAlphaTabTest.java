/*
 * $Id: NNffbpAlphaTabTest.java,v 1.3 2006/07/24 14:40:25 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.case2.util.nn;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

public class NNffbpAlphaTabTest extends TestCase {

    public void testThatNetIsLoadedCorrectly() {
        NNffbpAlphaTab tab = loadTestNet();

        // Test input/output unit count
        //
        assertEquals(6, tab.nn_in);
        assertEquals(1, tab.nn_out);

        // Test input min/max
        //
        assertEquals(6, tab.inmin.length);
        assertEquals(6, tab.inmax.length);
        double[][] inMinMax = {{-1.610930, 3.998400},
                               {-5.928960, 3.881530},
                               {-4.234020, 8.997880},
                               {-6.758290, 3.892520},
                               {-12.181100, 2.849470},
                               {-4.197060, 8.639600}};
        for (int i = 0; i < inMinMax.length; i++) {
            assertEquals(inMinMax[i][0], tab.inmin[i], 1.0e-6);
            assertEquals(inMinMax[i][1], tab.inmax[i], 1.0e-6);
        }

        // Test output min/max
        //
        assertEquals(1, tab.outmin.length);
        assertEquals(1, tab.outmax.length);
        double[] outMinMax = {0.000000, 1.000000};
        assertEquals(outMinMax[0], tab.outmin[0], 1.0e-6);
        assertEquals(outMinMax[1], tab.outmax[0], 1.0e-6);

        // Test that weights are correctly set by calculating an output
        //
        double[] nninp = new double[6];
        for (int i = 0; i < inMinMax.length; i++) {
            nninp[i] =  0.1 * (inMinMax[i][0] + inMinMax[i][1]);
        }
        final double[] nnout = tab.calc(nninp);
        assertEquals(1, nnout.length);
        assertEquals(0.813757, nnout[0], 1.0e-6);
    }

    private static NNffbpAlphaTab loadTestNet() {
        NNffbpAlphaTab tab = null;
        try {
            URL resource = NNffbpAlphaTabFastTest.class.getResource("nn_test.net");
            File testFile = new File(resource.toURI());
            tab = new NNffbpAlphaTab(testFile.getCanonicalPath());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to load test net.");
        }
        return tab;
    }
}
