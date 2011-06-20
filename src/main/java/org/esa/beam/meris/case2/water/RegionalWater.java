package org.esa.beam.meris.case2.water;

import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.meris.case2.algorithm.KMin;


public class RegionalWater extends WaterAlgorithm {

    private final double tsmExponent;
    private final double tsmFactor;
    private boolean outputAPoc;

    public RegionalWater(boolean outputAllKds, boolean outputAPoc, double spectrumOutOfScopeThreshold,
                         double tsmExponent, double tsmFactor,
                         double averageSalinity, double averageTemperature) {
        super(outputAllKds, spectrumOutOfScopeThreshold, averageSalinity, averageTemperature);
        this.outputAPoc = outputAPoc;
        this.tsmExponent = tsmExponent;
        this.tsmFactor = tsmFactor;
    }

    @Override
    protected KMin createKMin(WritableSample[] targetSamples) {
        final double bTsm = targetSamples[TARGET_BB_SPM_INDEX].getDouble() / BTSM_TO_SPM_FACTOR;
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        return new KMin(bTsm, aPig, aGelbstoff);
    }

    @Override
    protected double computeChiSquare(double[] forwardWaterOutput, double[] logRLw_cut) {
        double chiSquare = 0.0;
        for (int i = 0; i < forwardWaterOutput.length; i++) {
            chiSquare += Math.pow(forwardWaterOutput[i] - logRLw_cut[i], 2);
        }
        return chiSquare;
    }

    @Override
    protected double[] getForwardWaterInput(double solzen, double satzen, double azi_diff_deg,
                                            double averageTemperature, double averageSalinity, double[] waterOutnet) {
        double[] forwardWaterInnet = new double[10];
        forwardWaterInnet[0] = solzen;
        forwardWaterInnet[1] = satzen;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = averageTemperature;
        forwardWaterInnet[4] = averageSalinity;
        forwardWaterInnet[5] = waterOutnet[1]; // log_conc_apart
        forwardWaterInnet[6] = waterOutnet[2]; // log_conc_agelb
        forwardWaterInnet[7] = waterOutnet[3]; // log_conc_apig
        forwardWaterInnet[8] = waterOutnet[4]; // log_conc_bpart
        forwardWaterInnet[9] = waterOutnet[5]; // log_conc_bwit
        return forwardWaterInnet;

    }

    @Override
    protected void fillTargetSamples(double[] backwardWaterOutput, WritableSample[] targetSamples) {
        double chlConc = Math.exp(backwardWaterOutput[0]);

        targetSamples[TARGET_CHL_CONC_INDEX].set(chlConc);

        double aPart = Math.exp(backwardWaterOutput[1]);
        double aGelbstoff = Math.exp(backwardWaterOutput[2]);
        double aPig = Math.exp(backwardWaterOutput[3]);
        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(aGelbstoff);
        targetSamples[TARGET_A_PIGMENT_INDEX].set(aPig);
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff + aPart);

        double bTsm = Math.exp(backwardWaterOutput[4]);
        targetSamples[TARGET_BB_SPM_INDEX].set(bTsm * BTSM_TO_SPM_FACTOR);
        targetSamples[TARGET_TSM_INDEX].set(Math.exp(Math.log(tsmFactor) + backwardWaterOutput[4] * tsmExponent));

        if (outputAPoc) {
            // todo - How to compute a_poc_443?
            targetSamples[TARGET_A_POC_INDEX].set(0.0);
        }
    }

    @Override
    protected double[] getBackwardWaterInput(double solzen, double satzen, double azi_diff_deg, double averageSalinity,
                                             double averageTemperature, double[] logRLw) {
        double[] waterInnet = new double[16];
        waterInnet[0] = solzen;
        waterInnet[1] = satzen;
        waterInnet[2] = azi_diff_deg;
        waterInnet[3] = averageTemperature;
        waterInnet[4] = averageSalinity;
        waterInnet[5] = logRLw[0]; // 412 reflec_1
        waterInnet[6] = logRLw[1]; // 443 reflec_2
        waterInnet[7] = logRLw[2]; // 490 reflec_3
        waterInnet[8] = logRLw[3]; // 510 reflec_4
        waterInnet[9] = logRLw[4]; // 560 reflec_5
        waterInnet[10] = logRLw[5]; // 620 reflec_6
        waterInnet[11] = logRLw[6]; // 665 reflec_7
        waterInnet[12] = logRLw[8]; // 708 reflec_9
        waterInnet[13] = logRLw[9]; // 753 reflec_10
        waterInnet[14] = logRLw[10]; // 778 reflec_12
        waterInnet[15] = logRLw[11]; // 865 reflec_13
        return waterInnet;
    }

}
