/*
 * $Id: AlgorithmParameter.java,v 1.28 2007-06-05 09:59:02 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.merisc2r.algorithm;

import org.esa.beam.case2.algorithm.AlgorithmParameter;


public class Case2RAlgorithmParameter extends AlgorithmParameter {

    public Case2RAlgorithmParameter() {
        waterNnInverseFilePath = "./water_net_20040320/meris_bn_20040322_45x16x12x8x5_5177.9.net";
        waterNnForwardFilePath = "./water_net_20040320/meris_fn_20040319_15x15x15_1750.4.net";
    }
}

