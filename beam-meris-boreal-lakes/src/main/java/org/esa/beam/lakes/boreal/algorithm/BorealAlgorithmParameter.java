/*
 * $Id: BorealAlgorithmParameter.java,v 1.1 2007-06-05 11:32:00 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.lakes.boreal.algorithm;


import org.esa.beam.case2.algorithm.AlgorithmParameter;

public class BorealAlgorithmParameter extends AlgorithmParameter {

    public BorealAlgorithmParameter() {
        waterNnInverseFilePath = "./water_net_boreal_20080605/45x16x12x8_44.8.net";
        waterNnForwardFilePath = "./water_net_boreal_20080605/15x15x15_96.5.net";
        chlConversionFactor = 62.6;
        chlConversionExponent = 1.29;
        tsmConversionFactor = 1.0;
        tsmConversionExponent = 1.0;
    }
}

