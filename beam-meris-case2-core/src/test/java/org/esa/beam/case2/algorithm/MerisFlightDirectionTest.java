package org.esa.beam.case2.algorithm;

import org.junit.Test;

import static org.junit.Assert.*;

public class MerisFlightDirectionTest {

    @Test
    public void testFindNadirColumnInScene() {
        int expectedNadirIndex = 30;
        final double[] data = createViewZenithArray(expectedNadirIndex, 12.3);
        assertEquals(expectedNadirIndex, MerisFlightDirection.findNadirColumnIndex(data));
    }

    @Test
    public void testFindNadirColumnIfNadirOutOfSceneLeftSide() {
        int expectedNadirIndex = -243;
        final double[] data = createViewZenithArray(expectedNadirIndex, 30.6);
        assertEquals(expectedNadirIndex, MerisFlightDirection.findNadirColumnIndex(data));
    }


    @Test
    public void testFindNadirColumnIfNadirOutOfSceneRightSide() {
        int expectedNadirIndex = 765;
        final double[] data = createViewZenithArray(expectedNadirIndex, 48.76);
        assertEquals(expectedNadirIndex, MerisFlightDirection.findNadirColumnIndex(data));
    }

    private double[] createViewZenithArray(int expectedNadirIndex, double startValue) {
        final double[] data = new double[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.abs((-startValue / expectedNadirIndex) * i + startValue);
        }
        return data;
    }

}
