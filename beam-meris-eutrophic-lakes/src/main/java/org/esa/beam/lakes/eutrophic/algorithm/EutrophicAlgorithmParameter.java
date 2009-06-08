/*
 * $Id: EutrophicAlgorithmParameter.java,v 1.28 2007-06-05 09:59:02 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.lakes.eutrophic.algorithm;


import org.esa.beam.case2.algorithm.AlgorithmParameter;

public class EutrophicAlgorithmParameter extends AlgorithmParameter{

    public boolean outputABtsm = true; // "a_btsm"

    public EutrophicAlgorithmParameter() {
        waterNnInverseFilePath = "./water_net_eutrophic_20070710/60x20_586.8inv.net";
        waterNnForwardFilePath = "./water_net_eutrophic_20070710/30x15_88.8forw.net";
        atmCorrNnFilePath = "./atmo_net_20080515/25x30x35x40_4016.9.net";
        chlConversionFactor = 0.0318;
        chlConversionExponent = 1.0;
    }



}
