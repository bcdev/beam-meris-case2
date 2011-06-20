package org.esa.beam.meris.case2.water;

import org.esa.beam.atmosphere.operator.ReflectanceEnum;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.meris.case2.algorithm.KMin;
import org.esa.beam.nn.NNffbpAlphaTabFast;

public abstract class WaterAlgorithm {

    public static final int SOURCE_REFLEC_1_INDEX = 0;
    public static final int SOURCE_REFLEC_2_INDEX = 1;
    public static final int SOURCE_REFLEC_3_INDEX = 2;
    public static final int SOURCE_REFLEC_4_INDEX = 3;
    public static final int SOURCE_REFLEC_5_INDEX = 4;
    public static final int SOURCE_REFLEC_6_INDEX = 5;
    public static final int SOURCE_REFLEC_7_INDEX = 6;
    public static final int SOURCE_REFLEC_8_INDEX = 7;
    public static final int SOURCE_REFLEC_9_INDEX = 8;
    public static final int SOURCE_SOLAZI_INDEX = 9;
    public static final int SOURCE_SOLZEN_INDEX = 10;
    public static final int SOURCE_SATAZI_INDEX = 11;
    public static final int SOURCE_SATZEN_INDEX = 12;
    public static final int SOURCE_ZONAL_WIND_INDEX = 13;
    public static final int SOURCE_MERID_WIND_INDEX = 14;

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

    public static final double BTSM_TO_SPM_FACTOR = 0.02;

    private double spectrumOutOfScopeThreshold;

    protected WaterAlgorithm(double spectrumOutOfScopeThreshold) {
        this.spectrumOutOfScopeThreshold = spectrumOutOfScopeThreshold;
    }

    public double[] perform(NNffbpAlphaTabFast backwardWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                            double solzen, double satzen, double azi_diff_deg, Sample[] sourceSamples,
                            WritableSample[] targetSamples, ReflectanceEnum inputReflecAre) {
        // test RLw against lowest or cut value in NN and set in lower
        double[] RLw = new double[9];
        RLw[0] = sourceSamples[SOURCE_REFLEC_1_INDEX].getDouble();
        RLw[1] = sourceSamples[SOURCE_REFLEC_2_INDEX].getDouble();
        RLw[2] = sourceSamples[SOURCE_REFLEC_3_INDEX].getDouble();
        RLw[3] = sourceSamples[SOURCE_REFLEC_4_INDEX].getDouble();
        RLw[4] = sourceSamples[SOURCE_REFLEC_5_INDEX].getDouble();
        RLw[5] = sourceSamples[SOURCE_REFLEC_6_INDEX].getDouble();
        RLw[6] = sourceSamples[SOURCE_REFLEC_7_INDEX].getDouble();
        RLw[7] = sourceSamples[SOURCE_REFLEC_8_INDEX].getDouble();
        RLw[8] = sourceSamples[SOURCE_REFLEC_9_INDEX].getDouble();
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
        double[] backwardWaterInput = getBackwardWaterInput(solzen, satzen, azi_diff_deg, logRLw);

        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (isLogRLwOutOfRange(backwardWaterInput, backwardWaterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(WLR_OOR_BIT_INDEX, true);
        }

        /* calculate concentrations using the water nn */
        double[] backwardWaterOutput = backwardWaterNet.calc(backwardWaterInput);

        fillTargetSamples(backwardWaterOutput, targetSamples);

        /* test if concentrations are within training range */
        if (isWaterConcentrationOOR(backwardWaterOutput, backwardWaterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(CONC_OOR_BIT_INDEX, true);
        }

        /* do forward NN computation */
        double[] forwardWaterInput = getForwardWaterInput(solzen, satzen, azi_diff_deg, backwardWaterOutput);
        double[] forwardWaterOutput = forwardWaterNet.calc(forwardWaterInput);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = computeChiSquare(forwardWaterOutput, logRLw);

        targetSamples[TARGET_CHI_SQUARE_INDEX].set(chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            targetSamples[TARGET_FLAG_INDEX].set(OOTR_BIT_INDEX, true);
        }
        // compute k_min and z90_max RD 20060811
        final KMin kMin = createKMin(targetSamples);
        double k_min = kMin.computeKMinValue();
        targetSamples[TARGET_K_MIN_INDEX].set(k_min);
        targetSamples[TARGET_Z90_MAX_INDEX].set(-1.0 / k_min);

        targetSamples[TARGET_KD_490_INDEX].set(kMin.computeKd490());

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


    protected abstract KMin createKMin(WritableSample[] targetSamples);

    protected abstract double computeChiSquare(double[] forwardWaterOutput, double[] logRLw_cut);

    protected abstract double[] getForwardWaterInput(double solzen, double satzen, double azi_diff_deg,
                                                     double[] waterOutnet);

    protected abstract void fillTargetSamples(double[] backwardWaterOutput, WritableSample[] targetSamples);

    protected abstract double[] getBackwardWaterInput(double teta_sun_deg, double teta_view_deg, double azi_diff_deg,
                                                      double[] logRlw);


    /*-----------------------------------------------------------------------------------
     **	test water leaving radiances as input to neural network for out of training range
     **	if out of range set to lower or upper boundary value
    -----------------------------------------------------------------------------------*/
    private boolean isLogRLwOutOfRange(double[] backwardWaterInput, NNffbpAlphaTabFast inverseWaterNet) {
        final double[] inmax = inverseWaterNet.getInmax();
        final double[] inmin = inverseWaterNet.getInmin();
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
