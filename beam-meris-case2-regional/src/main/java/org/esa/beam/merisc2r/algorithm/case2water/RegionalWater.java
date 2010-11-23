package org.esa.beam.merisc2r.algorithm.case2water;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.KMin;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.algorithm.water.WaterAlgorithm;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;


public class RegionalWater extends WaterAlgorithm {

    private double tsmExponent;
    private double tsmFactor;
    private double chlExponent;
    private double chlFactor;

    @Override
    public void init(NNffbpAlphaTabFast inverseWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                     AlgorithmParameter parameter) {
        super.init(inverseWaterNet, forwardWaterNet, parameter);
        tsmExponent = parameter.tsmConversionExponent;
        tsmFactor = parameter.tsmConversionFactor;
        chlExponent = parameter.chlConversionExponent;
        chlFactor = parameter.chlConversionFactor;
    }

    @Override
    protected double computeKMin(OutputBands outputBands) {
        final double bTsm = outputBands.getDoubleValue("b_tsm");
        final double aPig = outputBands.getDoubleValue("a_pig");
        final double aGelbstoff = outputBands.getDoubleValue("a_gelbstoff");
        final KMin kMin = new KMin();
        return kMin.perform(bTsm, aPig, aGelbstoff);
    }

    @Override
    protected double computeChiSquare(double[] forwardWaterOutnet, double[] RLw_cut) {
        return Math.pow(forwardWaterOutnet[0] - Math.log(RLw_cut[0]), 2) +
               Math.pow(forwardWaterOutnet[1] - Math.log(RLw_cut[1]), 2) +
               Math.pow(forwardWaterOutnet[2] - Math.log(RLw_cut[2]), 2) +
               Math.pow(forwardWaterOutnet[3] - Math.log(RLw_cut[3]), 2) +
               Math.pow(forwardWaterOutnet[4] - Math.log(RLw_cut[4]), 2) +
               Math.pow(forwardWaterOutnet[5] - Math.log(RLw_cut[5]), 2) +
               Math.pow(forwardWaterOutnet[6] - Math.log(RLw_cut[6]), 2) +
               Math.pow(forwardWaterOutnet[7] - Math.log(RLw_cut[8]), 2); // in outnet 7 corresponds to RLw 8
    }

    @Override
    protected double[] getForwardWaterInnet(double teta_sun_deg, double teta_view_deg, double azi_diff_deg,
                                            double[] waterOutnet) {
        double[] forwardWaterInnet = new double[6];
        forwardWaterInnet[0] = teta_sun_deg;
        forwardWaterInnet[1] = teta_view_deg;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log gelbstoff
        forwardWaterInnet[4] = waterOutnet[1]; // log pigment
        forwardWaterInnet[5] = waterOutnet[2]; // log tsm
        return forwardWaterInnet;
    }

    @Override
    protected void fillOutput(double[] waterOutnet, OutputBands outputBands) {
        double bTsm = Math.exp(waterOutnet[0]);
        outputBands.setValue("b_tsm", bTsm);
        outputBands.setValue("tsm", Math.exp(Math.log(tsmFactor) + waterOutnet[0] * tsmExponent));

        double aPig = Math.exp(waterOutnet[1]);
        outputBands.setValue("a_pig", aPig);
        outputBands.setValue("chl_conc", Math.exp(Math.log(chlFactor) + waterOutnet[1] * chlExponent));

        double aGelbstoff = Math.exp(waterOutnet[2]);
        outputBands.setValue("a_gelbstoff", aGelbstoff);
        outputBands.setValue("a_total", aPig + aGelbstoff);
    }

    @Override
    protected double[] getWaterInnet(double teta_sun_deg, double teta_view_deg, double azi_diff_deg, double[] RLw_cut) {
        double[] waterInnet = new double[11];
        waterInnet[0] = teta_sun_deg;
        waterInnet[1] = teta_view_deg;
        waterInnet[2] = azi_diff_deg;

        for (int i = 3; i < 10; i++) {
            waterInnet[i] = Math.log(RLw_cut[i - 3]); /* bands 1-7 == 412 - 664 nm */
        }
        waterInnet[10] = Math.log(RLw_cut[8]); /* band 708 nm */
        return waterInnet;
    }

    @Override
    protected double getCutThreshold(double[] inmin) {
        double cut_thresh = 1000.0;
        for (int i = 3; i < 11; i++) {
            double inmini = Math.exp(inmin[i]);
            if (inmini < cut_thresh) {
                cut_thresh = inmini;
            }
        }
        return cut_thresh;
    }
}
