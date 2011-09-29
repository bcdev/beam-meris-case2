package org.esa.beam.meris.case2.water;

import org.esa.beam.atmosphere.operator.ReflectanceEnum;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.meris.case2.algorithm.KMin;
import org.esa.beam.nn.NNffbpAlphaTabFast;

public class WaterAlgorithm {

    public static final int SOURCE_REFLEC_1_INDEX = 0;
    public static final int SOURCE_REFLEC_2_INDEX = 1;
    public static final int SOURCE_REFLEC_3_INDEX = 2;
    public static final int SOURCE_REFLEC_4_INDEX = 3;
    public static final int SOURCE_REFLEC_5_INDEX = 4;
    public static final int SOURCE_REFLEC_6_INDEX = 5;
    public static final int SOURCE_REFLEC_7_INDEX = 6;
    public static final int SOURCE_REFLEC_8_INDEX = 7;
    public static final int SOURCE_REFLEC_9_INDEX = 8;
    public static final int SOURCE_REFLEC_10_INDEX = 9;
    public static final int SOURCE_REFLEC_12_INDEX = 10;
    public static final int SOURCE_REFLEC_13_INDEX = 11;
    public static final int SOURCE_SOLAZI_INDEX = 12;
    public static final int SOURCE_SOLZEN_INDEX = 13;
    public static final int SOURCE_SATAZI_INDEX = 14;
    public static final int SOURCE_SATZEN_INDEX = 15;
    public static final int SOURCE_ZONAL_WIND_INDEX = 16;
    public static final int SOURCE_MERID_WIND_INDEX = 17;

    public static final int TARGET_A_GELBSTOFF_INDEX = 0;
    public static final int TARGET_A_PIGMENT_INDEX = 1;
    public static final int TARGET_A_TOTAL_INDEX = 2;
    public static final int TARGET_A_POC_INDEX = 3;
    public static final int TARGET_BB_SPM_INDEX = 4;
    public static final int TARGET_TSM_INDEX = 5;
    public static final int TARGET_CHL_CONC_INDEX = 6;
    public static final int TARGET_CHI_SQUARE_INDEX = 7;
    public static final int TARGET_K_MIN_INDEX = 8;
    public static final int TARGET_Z90_MAX_INDEX = 9;
    public static final int TARGET_KD_490_INDEX = 10;
    public static final int TARGET_TURBIDITY_INDEX_INDEX = 11;
    public static final int TARGET_FLAG_INDEX = 12;
    public static final int TARGET_A_GELBSTOFF_FIT_INDEX = 13;
    public static final int TARGET_A_GELBSTOFF_FIT_MAX_INDEX = 14;
    public static final int TARGET_A_GELBSTOFF_FIT_MIN_INDEX = 15;
    public static final int TARGET_A_PIG_FIT_INDEX = 16;
    public static final int TARGET_A_PIG_FIT_MAX_INDEX = 17;
    public static final int TARGET_A_PIG_FIT_MIN_INDEX = 18;
    public static final int TARGET_B_TSM_FIT_INDEX = 19;
    public static final int TARGET_B_TSM_FIT_MAX_INDEX = 20;
    public static final int TARGET_B_TSM_FIT_MIN_INDEX = 21;
    public static final int TARGET_TSM_FIT_INDEX = 22;
    public static final int TARGET_CHL_CONC_FIT_INDEX = 23;
    public static final int TARGET_CHI_SQUARE_FIT_INDEX = 24;
    public static final int TARGET_N_ITER_FIT_INDEX = 25;
    public static final int TARGET_PARAM_CHANGE_FIT_INDEX = 26;
    public static final int TARGET_KD_SPECTRUM_START_INDEX = 50;

    public static final int WLR_OOR_BIT_INDEX = 0;
    public static final int CONC_OOR_BIT_INDEX = 1;
    public static final int OOTR_BIT_INDEX = 2;
    public static final int WHITECAPS_BIT_INDEX = 3;
    public static final int FIT_FAILED_INDEX = 4;
    public static final int INVALID_BIT_INDEX = 7;

    private static final double RLW620_MAX = 0.03823;
    private static final double TURBIDITY_AT = 174.41;
    private static final double TURBIDITY_BT = 0.39;
    private static final double TURBIDITY_C = 0.1533;

    public static final double BTSM_TO_SPM_FACTOR = 0.01;

    private final double spectrumOutOfScopeThreshold;
    private double averageSalinity;
    private double averageTemperature;
    private boolean outputKdSpectrum;

    private final double tsmExponent;
    private final double tsmFactor;
    private boolean outputAPoc;

    public WaterAlgorithm(boolean outputAllKds, boolean outputAPoc, double spectrumOutOfScopeThreshold,
                          double tsmExponent, double tsmFactor,
                          double averageSalinity, double averageTemperature) {
        this.outputKdSpectrum = outputAllKds;
        this.outputAPoc = outputAPoc;
        this.spectrumOutOfScopeThreshold = spectrumOutOfScopeThreshold;
        this.tsmExponent = tsmExponent;
        this.tsmFactor = tsmFactor;
        this.averageSalinity = averageSalinity;
        this.averageTemperature = averageTemperature;

    }

    public double[] perform(NNffbpAlphaTabFast inverseWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                            double solzen, double satzen, double azi_diff_deg, Sample[] sourceSamples,
                            WritableSample[] targetSamples, ReflectanceEnum inputReflecAre) {
        // test RLw against lowest or cut value in NN and set in lower
        double[] RLw = new double[12];
        RLw[0] = sourceSamples[SOURCE_REFLEC_1_INDEX].getDouble();
        RLw[1] = sourceSamples[SOURCE_REFLEC_2_INDEX].getDouble();
        RLw[2] = sourceSamples[SOURCE_REFLEC_3_INDEX].getDouble();
        RLw[3] = sourceSamples[SOURCE_REFLEC_4_INDEX].getDouble();
        RLw[4] = sourceSamples[SOURCE_REFLEC_5_INDEX].getDouble();
        RLw[5] = sourceSamples[SOURCE_REFLEC_6_INDEX].getDouble();
        RLw[6] = sourceSamples[SOURCE_REFLEC_7_INDEX].getDouble();
        RLw[7] = sourceSamples[SOURCE_REFLEC_8_INDEX].getDouble();
        RLw[8] = sourceSamples[SOURCE_REFLEC_9_INDEX].getDouble();
        RLw[9] = sourceSamples[SOURCE_REFLEC_10_INDEX].getDouble();
        RLw[10] = sourceSamples[SOURCE_REFLEC_12_INDEX].getDouble();
        RLw[11] = sourceSamples[SOURCE_REFLEC_13_INDEX].getDouble();
        if (ReflectanceEnum.IRRADIANCE_REFLECTANCES.equals(inputReflecAre)) {
            for (int i = 0; i < RLw.length; i++) {
                RLw[i] /= Math.PI;
            }
        }
        double[] logRLw = new double[RLw.length];
        for (int i = 0; i < RLw.length; i++) {
            logRLw[i] = Math.log(RLw[i]);
        }


        /* prepare for water net */
        double[] backwardWaterInput = getBackwardWaterInput(solzen, satzen, azi_diff_deg, averageSalinity,
                                                            averageTemperature,
                                                            logRLw);

        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (isInputInTrainigRange(backwardWaterInput, inverseWaterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(WLR_OOR_BIT_INDEX, true);
        }

        /* calculate concentrations using the water nn */
        double[] backwardWaterOutput = inverseWaterNet.calc(backwardWaterInput);

        fillTargetSamples(backwardWaterOutput, targetSamples);

        /* test if concentrations are within training range */
        if (isWaterConcentrationOOR(backwardWaterOutput, inverseWaterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(CONC_OOR_BIT_INDEX, true);
        }

        /* do forward NN computation */
        double[] forwardWaterInput = getForwardWaterInput(solzen, satzen, azi_diff_deg, averageTemperature,
                                                          averageSalinity, backwardWaterOutput
        );
        double[] forwardWaterOutput = forwardWaterNet.calc(forwardWaterInput);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = computeChiSquare(forwardWaterOutput, logRLw);

        targetSamples[TARGET_CHI_SQUARE_INDEX].set(chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            targetSamples[TARGET_FLAG_INDEX].set(OOTR_BIT_INDEX, true);
        }
        // compute k_min and z90_max RD 20060811
        final KMin kMin = createKMin(targetSamples);
        // todo - What shall we use?
        // If we use the k_min computed by the neural net, it won't be consistent with the kd-spectrum
        // If we use the k_min from the class KMin we have a huge difference
//        double k_min = kMin.computeKMinValue();
        double k_min = Math.exp(backwardWaterOutput[6]);
        targetSamples[TARGET_K_MIN_INDEX].set(k_min);
        targetSamples[TARGET_Z90_MAX_INDEX].set(-1.0 / k_min);

        if (outputKdSpectrum) {
            double[] kdSpectrum = kMin.computeKdSpectrum();
            for (int i = 0; i < kdSpectrum.length; i++) {
                double aKdSpectrum = kdSpectrum[i];
                targetSamples[TARGET_KD_SPECTRUM_START_INDEX + i].set(aKdSpectrum);
            }
        } else {
            targetSamples[TARGET_KD_490_INDEX].set(kMin.computeKd490());
        }

        final double turbidity = computeTurbidityIndex(RLw[5]);// parameter Rlw at 620 'reflec_6'
        targetSamples[TARGET_TURBIDITY_INDEX_INDEX].set(turbidity);
        return logRLw;

    }

    private double computeTurbidityIndex(double rlw620) {
        if (rlw620 > RLW620_MAX) {  // maximum value for computing the turbidity Index
            rlw620 = RLW620_MAX;
        }
        double rho = rlw620 * Math.PI;
        return TURBIDITY_AT * rho / (1 - rho / TURBIDITY_C) + TURBIDITY_BT;
    }


    private KMin createKMin(WritableSample[] targetSamples) {
        final double bTsm = targetSamples[TARGET_BB_SPM_INDEX].getDouble() / BTSM_TO_SPM_FACTOR;
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        return new KMin(bTsm, aPig, aGelbstoff);
    }

    private double computeChiSquare(double[] forwardWaterOutput, double[] logRLw_cut) {
        double chiSquare = 0.0;
        for (int i = 0; i < forwardWaterOutput.length; i++) {
            chiSquare += Math.pow(forwardWaterOutput[i] - logRLw_cut[i], 2);
        }
        return chiSquare;
    }

    private double[] getForwardWaterInput(double solzen, double satzen, double azi_diff_deg,
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

    private void fillTargetSamples(double[] backwardWaterOutput, WritableSample[] targetSamples) {
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

    private double[] getBackwardWaterInput(double solzen, double satzen, double azi_diff_deg, double averageSalinity,
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

    /*-----------------------------------------------------------------------------------
     **	test water leaving radiances as input to neural network for out of training range
     **	if out of range set to lower or upper boundary value
    -----------------------------------------------------------------------------------*/
    private boolean isInputInTrainigRange(double[] backwardWaterInput, NNffbpAlphaTabFast backwardWaterNet) {
        final double[] inmax = backwardWaterNet.getInmax();
        final double[] inmin = backwardWaterNet.getInmin();
        boolean isOutOfRange = false;
        for (int i = 0; i < backwardWaterInput.length; i++) {
            if (backwardWaterInput[i] > inmax[i]) {
                backwardWaterInput[i] = inmax[i];
                isOutOfRange |= true;
            }
            if (backwardWaterInput[i] < inmin[i]) {
                backwardWaterInput[i] = inmin[i];
                isOutOfRange |= true;
            }
        }
        return isOutOfRange;
    }

    /*-------------------------------------------------------------------------------
     **	test water constituents as output of neural network for out of training range
     **
    --------------------------------------------------------------------------------*/
    private boolean isWaterConcentrationOOR(double[] backwardWaterOutput, NNffbpAlphaTabFast backwardWaterNet) {
        final double[] outmax = backwardWaterNet.getOutmax();
        final double[] outmin = backwardWaterNet.getOutmin();
        for (int i = 0; i < outmin.length; i++) {
            double min = outmin[i];
            double max = outmax[i];
            double value = backwardWaterOutput[i];
            if (value > max || value < min) {
                return true;
            }
        }
        return false;
    }
}
