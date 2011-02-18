package org.esa.beam.meris.case2.algorithm;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class KMinTest {
    ///////////////////////////////////////////////////////////
    // This test checks against known values
    // The values are taken from a stable state
    ///////////////////////////////////////////////////////////

    private static double bTsm;
    private static double aPig;
    private static double aGelbstoff;
    private static double aBtsm;

    @BeforeClass
    public static void beforeClass() throws Exception {
        bTsm = 2.989373;
        aPig = 0.144818;
        aGelbstoff = 0.142277;
        aBtsm = 2.989373;
    }

    @Test
    public void testComputationWithABtsm() throws Exception {
        KMin kMin = new KMin(bTsm, aPig, aGelbstoff, aBtsm);
        assertEquals(1.405447, kMin.computeKMinValue(), 1.0e-6);
        assertEquals(2.354699, kMin.computeKd490(), 1.0e-6);

    }

    @Test
    public void testComputationWithoutABtsm() throws Exception {
        KMin kMin = new KMin(bTsm, aPig, aGelbstoff);
        assertEquals(0.255409, kMin.computeKMinValue(), 1.0e-6);
        assertEquals(0.283257, kMin.computeKd490(), 1.0e-6);
    }
}
