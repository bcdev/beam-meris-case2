package org.esa.beam.case2.util.nn;

/*
 * $Id: NNffbpAlphaTabFastTest.java,v 1.2 2006/07/24 14:40:25 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

public class NNffbpAlphaTabFastTest extends TestCase {


    public void testCalcJacobi() {
        final NNffbpAlphaTabFast tab = loadTestNet();

        final double[] nnInput = new double[]{1.0, 3.4, 6.988, 4.4, 7.0, 16.21};

        final NNCalc nnCalc = tab.calcJacobi(nnInput);

        final double[] expNnOutput = new double[]{0.9999546066706964};
            assertEquals(expNnOutput[0], nnCalc.nnOutput[0], 1e-6);


        final double[][] expJacobiMatrix = new double[][]{
            {
                -7.3325278006568306E-6, 3.2639459486171825E-6, 7.527711538784537E-6,
                -8.186466522910375E-6, 9.557482758745628E-6, 1.2507178214659703E-5
            }
        };
        for (int i = 0; i < nnCalc.jacobiMatrix.length; i++) {
            for (int j = 0; j < nnCalc.jacobiMatrix[i].length; j++) {
                assertEquals(expJacobiMatrix[i][j], nnCalc.jacobiMatrix[i][j], 1e-6);
            }
        }

    }

    private static NNffbpAlphaTabFast loadTestNet() {
        NNffbpAlphaTabFast tabFast = null;
        try {
            URL resource = NNffbpAlphaTabFastTest.class.getResource("nn_test.net");
            File testFile = new File(resource.toURI());
            tabFast = new NNffbpAlphaTabFast(testFile.getCanonicalPath());
        } catch (Exception e) {
            fail("Failed to load test net.");
            e.printStackTrace();
        }
        return tabFast;
    }
}