package org.esa.beam.meris.case2;

import org.junit.Test;

import static org.junit.Assert.*;

public class MerisCase2BasisWaterOpTest {

    @Test
    public void testGetAzimuthDifference() throws Exception {
        assertEquals(172.0, MerisCase2BasisWaterOp.getAzimuthDifference(92.0, 100.0), 1.0e-8);
        assertEquals(172.0, MerisCase2BasisWaterOp.getAzimuthDifference(100.0, 92.0), 1.0e-8);
        assertEquals(10.0, MerisCase2BasisWaterOp.getAzimuthDifference(90.0, 280.0), 1.0e-8);
        assertEquals(10.0, MerisCase2BasisWaterOp.getAzimuthDifference(280.0, 90.0), 1.0e-8);
    }
}
