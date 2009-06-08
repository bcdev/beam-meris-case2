/*
 * $Id: FormattedReaderTest.java,v 1.3 2006/07/24 14:40:25 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.case2.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.net.URL;

public class FormattedReaderTest extends TestCase {
    private static final double EPS = 1.0e-8;

    private FormattedReader r;

    @Override
    protected void setUp() throws Exception {
        URL resource = getClass().getResource("fr_test.txt");
        File testFile = new File(resource.toURI());
        r = new FormattedReader(testFile.getCanonicalPath());
    }

    @Override
    protected void tearDown() throws Exception {
        r.close();
        r = null;
    }

    public void testX() throws IOException {

        assertEquals("ranges repeated for easier input", r.rString());
        assertEquals(6, r.rlong());
        assertEquals(-1.61093, r.rdouble(), EPS);
        assertEquals(3.9984, r.rdouble(), EPS);

        final double[] d4 = r.rdouble(4);
        assertEquals(4, d4.length);
        assertEquals(-5.92896, d4[0], EPS);
        assertEquals(3.88153, d4[1], EPS);
        assertEquals(-4.23402, d4[2], EPS);
        assertEquals(8.99788, d4[3], EPS);

        final long[] l3 = r.rlong(3);
        assertEquals(3, l3.length);
        assertEquals(198134, l3[0]);
        assertEquals(4493, l3[1]);
        assertEquals(-72345, l3[2]);

        assertEquals("$", r.rString());
        assertEquals("bias 1 50", r.rString());
        final double[][] d23 = r.rdoubleAll();
        assertEquals(2, d23.length);
        assertEquals(3, d23[0].length);
        assertEquals(3, d23[1].length);
        assertEquals(7.890334, d23[0][0], EPS);
        assertEquals(8.897559, d23[0][1], EPS);
        assertEquals(8.359957, d23[0][2], EPS);
        assertEquals(-23.919953, d23[1][0], EPS);
        assertEquals(44.968232, d23[1][1], EPS);
        assertEquals(4.968232, d23[1][2], EPS);
    }
}
