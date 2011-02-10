package org.esa.beam.meris.case2.water;

import org.esa.beam.framework.gpf.experimental.PointOperator;
import org.esa.beam.meris.case2.algorithm.KMin;


public class RegionalWater extends WaterAlgorithm {

    private final double tsmExponent;
    private final double tsmFactor;
    private final double chlExponent;
    private final double chlFactor;

    public RegionalWater(double spectrumOutOfScopeThreshold, double tsmExponent,
                         double tsmFactor, double chlExponent, double chlFactor) {
        super(spectrumOutOfScopeThreshold);
        this.tsmExponent = tsmExponent;
        this.tsmFactor = tsmFactor;
        this.chlExponent = chlExponent;
        this.chlFactor = chlFactor;
    }

    @Override
    protected KMin createKMin(PointOperator.WritableSample[] targetSamples) {
        final double bTsm = targetSamples[TARGET_B_TSM_INDEX].getDouble();
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        return new KMin(bTsm, aPig, aGelbstoff);
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
    protected double[] getForwardWaterInnet(double solzen, double satzen, double azi_diff_deg,
                                            double[] waterOutnet) {
        double[] forwardWaterInnet = new double[6];
        forwardWaterInnet[0] = solzen;
        forwardWaterInnet[1] = satzen;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log gelbstoff
        forwardWaterInnet[4] = waterOutnet[1]; // log pigment
        forwardWaterInnet[5] = waterOutnet[2]; // log tsm
        return forwardWaterInnet;

    }

    @Override
    protected void fillOutput(double[] waterOutnet, PointOperator.WritableSample[] targetSamples) {
        double bTsm = Math.exp(waterOutnet[0]);
        targetSamples[TARGET_B_TSM_INDEX].set(bTsm);
        targetSamples[TARGET_TSM_INDEX].set(Math.exp(Math.log(tsmFactor) + waterOutnet[0] * tsmExponent));

        double aPig = Math.exp(waterOutnet[1]);
        targetSamples[TARGET_A_PIGMENT_INDEX].set(aPig);
        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(Math.log(chlFactor) + waterOutnet[1] * chlExponent));

        double aGelbstoff = Math.exp(waterOutnet[2]);
        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(aGelbstoff);
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff);

    }

    @Override
    protected double[] getWaterInnet(double solzen, double satzen, double azi_diff_deg, double[] RLw_cut) {
        double[] waterInnet = new double[11];
        waterInnet[0] = solzen;
        waterInnet[1] = satzen;
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
