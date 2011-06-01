package org.esa.beam.meris.case2.water;

import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.meris.case2.algorithm.KMin;


public class BorealWater extends WaterAlgorithm {


    public BorealWater(double spectrumOutOfScopeThreshold) {
        super(spectrumOutOfScopeThreshold, 0, 0);
    }

    @Override
    protected KMin createKMin(WritableSample[] targetSamples) {
        final double bTsm = targetSamples[TARGET_BB_SPM_INDEX].getDouble() / BTSM_TO_SPM_FACTOR;
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        return new KMin(bTsm, aPig, aGelbstoff);
    }

    @Override
    protected double computeChiSquare(double[] forwardWaterOutnet, double[] logRLw_cut) {
        return Math.pow(forwardWaterOutnet[0] - logRLw_cut[0], 2) +
               Math.pow(forwardWaterOutnet[1] - logRLw_cut[1], 2) +
               Math.pow(forwardWaterOutnet[2] - logRLw_cut[2], 2) +
               Math.pow(forwardWaterOutnet[3] - logRLw_cut[3], 2) +
               Math.pow(forwardWaterOutnet[4] - logRLw_cut[4], 2) +
               Math.pow(forwardWaterOutnet[5] - logRLw_cut[5], 2) +
               Math.pow(forwardWaterOutnet[6] - logRLw_cut[6], 2) +
               Math.pow(forwardWaterOutnet[8] - logRLw_cut[8], 2);
    }

    @Override
    protected double[] getForwardWaterInnet(double solzen, double satzen, double azi_diff_deg,
                                            double averageTemperature, double averageSalinity, double[] waterOutnet) {
        double[] forwardWaterInnet = new double[6];
        forwardWaterInnet[0] = solzen;
        forwardWaterInnet[1] = satzen;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log bTsm
        forwardWaterInnet[4] = waterOutnet[1]; // log aPig
        forwardWaterInnet[5] = waterOutnet[2]; // log aGelbstoff
        return forwardWaterInnet;

    }

    @Override
    protected void fillOutput(double[] waterOutnet, WritableSample[] targetSamples) {
        double bTsm = Math.exp(waterOutnet[0]);
        targetSamples[TARGET_BB_SPM_INDEX].set(bTsm * BTSM_TO_SPM_FACTOR);
        targetSamples[TARGET_TSM_INDEX].set(bTsm / 0.95);

        double aPig = Math.exp(waterOutnet[1]);
        targetSamples[TARGET_A_PIGMENT_INDEX].set(aPig);
        targetSamples[TARGET_CHL_CONC_INDEX].set(62.6 * Math.pow(aPig, 1.29));

        double aGelbstoff = Math.exp(waterOutnet[2]);
        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(aGelbstoff);
        targetSamples[TARGET_A_TOTAL_INDEX].set(
                aPig + aGelbstoff + bTsm / 0.95 * 0.089); // all water constituents absorption
        // todo - How to compute a_poc_443?
        targetSamples[TARGET_A_POC_INDEX].set(0.0);
    }

    @Override
    protected double[] getWaterInnet(double solzen, double satzen, double azi_diff_deg, double averageSalinity,
                                     double averageTemperature, double[] logRLw_cut) {
        double[] waterInnet = new double[11];
        waterInnet[0] = solzen;
        waterInnet[1] = satzen;
        waterInnet[2] = azi_diff_deg;

        for (int i = 3; i < 10; i++) {
            waterInnet[i] = logRLw_cut[i - 3]; /* bands 1-7 == 412 - 664 nm */
        }
        waterInnet[10] = logRLw_cut[8]; /* band 708 nm */
        return waterInnet;
    }

}
