package org.esa.beam.meris.case2.water;

import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.meris.case2.algorithm.KMin;


public class EutrophicWater extends WaterAlgorithm {

    public static final int TARGET_A_BTSM_INDEX = 10;

    private final double tsmExponent;
    private final double tsmFactor;
    private final double chlExponent;
    private final double chlFactor;

    public EutrophicWater(double spectrumOutOfScopeThreshold, double tsmExponent,
                          double tsmFactor, double chlExponent, double chlFactor) {
        super(false, spectrumOutOfScopeThreshold, 0, 0);
        this.tsmExponent = tsmExponent;
        this.tsmFactor = tsmFactor;
        this.chlExponent = chlExponent;
        this.chlFactor = chlFactor;
    }

    @Override
    protected KMin createKMin(WritableSample[] targetSamples) {
        final double bTsm = targetSamples[TARGET_BB_SPM_INDEX].getDouble() / BTSM_TO_SPM_FACTOR;
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        final double aBtsm = targetSamples[TARGET_A_BTSM_INDEX].getDouble();

        final KMin kMin = new KMin(bTsm, aPig, aGelbstoff, aBtsm);
        kMin.setA_gelb_mer8(new double[]{
                1.9220648, 0.9934217, 0.3501478, 0.2260046,
                0.0832423, 0.0753961, 0.0201853, 0.0075169
        });
        return kMin;
    }

    @Override
    protected double computeChiSquare(double[] forwardWaterOutput, double[] logRLw_cut) {
        return Math.pow(forwardWaterOutput[0] - logRLw_cut[1], 2) + // it starts with 442 nm
               Math.pow(forwardWaterOutput[1] - logRLw_cut[2], 2) +
               Math.pow(forwardWaterOutput[2] - logRLw_cut[3], 2) +
               Math.pow(forwardWaterOutput[3] - logRLw_cut[4], 2) +
               Math.pow(forwardWaterOutput[4] - logRLw_cut[5], 2) +
               Math.pow(forwardWaterOutput[5] - logRLw_cut[6], 2) +
               Math.pow(forwardWaterOutput[6] - logRLw_cut[8], 2);
    }

    @Override
    protected double[] getForwardWaterInput(double solzen, double satzen, double azi_diff_deg,
                                            double averageTemperature, double averageSalinity, double[] waterOutnet) {
        double[] forwardWaterInnet = new double[7];
        forwardWaterInnet[0] = solzen;
        forwardWaterInnet[1] = satzen;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log gelbstoff
        forwardWaterInnet[4] = waterOutnet[1]; // log a_btsm
        forwardWaterInnet[5] = waterOutnet[2]; // log pigment
        forwardWaterInnet[6] = waterOutnet[3]; // log tsm
        return forwardWaterInnet;


    }

    @Override
    protected void fillTargetSamples(double[] backwardWaterOutput, WritableSample[] targetSamples) {
        double bTsm = Math.exp(backwardWaterOutput[3]);
        targetSamples[TARGET_BB_SPM_INDEX].set(bTsm * BTSM_TO_SPM_FACTOR);
        targetSamples[TARGET_TSM_INDEX].set(Math.exp(Math.log(tsmFactor) + backwardWaterOutput[3] * tsmExponent));

        double aPig = Math.exp(backwardWaterOutput[2]) * chlFactor;
        targetSamples[TARGET_A_PIGMENT_INDEX].set(aPig);
//        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(Math.log(chlFactor) + waterOutnet[2] * chlExponent));
        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(Math.log(1.0) + backwardWaterOutput[2] * chlExponent));

        double aGelbstoff = Math.exp(backwardWaterOutput[0]);
        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(aGelbstoff);
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff);

        double aBtsm = Math.exp(backwardWaterOutput[1]);
        targetSamples[TARGET_A_BTSM_INDEX].set(bTsm); // bleached suspended matter absorption at 442
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff + aBtsm);
        // todo - How to compute a_poc_443?
        targetSamples[TARGET_A_POC_INDEX].set(0.0);
    }

    @Override
    protected double[] getBackwardWaterInput(double solzen, double satzen, double azi_diff_deg, double averageSalinity,
                                             double averageTemperature, double[] logRLw_cut) {
        double[] waterInnet = new double[10];

        for (int i = 0; i < 6; i++) {
            waterInnet[i] = logRLw_cut[i + 1]; /* bands 2-7 == 442 - 664 nm */
        }
        waterInnet[6] = logRLw_cut[8]; /* band 708 nm */
        waterInnet[7] = solzen;
        waterInnet[8] = satzen;
        waterInnet[9] = azi_diff_deg;

        return waterInnet;

    }

}
