package org.esa.beam.meris.case2.water;

import org.esa.beam.atmosphere.operator.ReflectanceEnum;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.meris.case2.algorithm.KMin;
import org.esa.beam.meris.case2.util.NNInputMapper;
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
    public static final int TARGET_A_DET_INDEX = 2;
    public static final int TARGET_A_TOTAL_INDEX = 3;
    public static final int TARGET_A_POC_INDEX = 4;
    public static final int TARGET_B_TSM_INDEX = 5;
    public static final int TARGET_B_WHIT_INDEX = 6;
    public static final int TARGET_BB_SPM_INDEX = 7;
    public static final int TARGET_TSM_INDEX = 8;
    public static final int TARGET_CHL_CONC_INDEX = 9;
    public static final int TARGET_CHI_SQUARE_INDEX = 10;
    public static final int TARGET_K_MIN_INDEX = 11;
    public static final int TARGET_Z90_MAX_INDEX = 12;
    public static final int TARGET_KD_490_INDEX = 13;
    public static final int TARGET_TURBIDITY_INDEX_INDEX = 14;
    public static final int TARGET_FLAG_INDEX = 15;
    public static final int TARGET_A_GELBSTOFF_FIT_INDEX = 16;
    public static final int TARGET_A_GELBSTOFF_FIT_MAX_INDEX = 17;
    public static final int TARGET_A_GELBSTOFF_FIT_MIN_INDEX = 18;
    public static final int TARGET_A_PIG_FIT_INDEX = 19;
    public static final int TARGET_A_PIG_FIT_MAX_INDEX = 20;
    public static final int TARGET_A_PIG_FIT_MIN_INDEX = 21;
    public static final int TARGET_B_TSM_FIT_INDEX = 22;
    public static final int TARGET_B_TSM_FIT_MAX_INDEX = 23;
    public static final int TARGET_B_TSM_FIT_MIN_INDEX = 24;
    public static final int TARGET_TSM_FIT_INDEX = 25;
    public static final int TARGET_CHL_CONC_FIT_INDEX = 26;
    public static final int TARGET_CHI_SQUARE_FIT_INDEX = 27;
    public static final int TARGET_N_ITER_FIT_INDEX = 28;
    public static final int TARGET_PARAM_CHANGE_FIT_INDEX = 29;
    public static final int TARGET_SALINITY_INDEX = 30;
    public static final int TARGET_TEMPERATURE_INDEX = 31;
    public static final int TARGET_KD_SPECTRUM_START_INDEX = 32;

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

    private final boolean outputKdSpectrum;
    private final boolean outputAPoc;
    private final double spectrumOutOfScopeThreshold;
    private final double tsmExponent;
    private final double tsmFactor;
    private final double chlExponent;
    private final double chlFactor;
    private final ReflectanceEnum inputReflecAre;


    private final ThreadLocal<NNffbpAlphaTabFast> threadLocalForwardIopNet;
    private final ThreadLocal<NNffbpAlphaTabFast> threadLocalInverseIopNet;
    private final NNInputMapper invIopMapper;
    private final ThreadLocal<NNffbpAlphaTabFast> threadLocalInverseKdNet;
    private final NNInputMapper invKdMapper;

    public WaterAlgorithm(boolean outputAllKds, boolean outputAPoc, double spectrumOutOfScopeThreshold,
                          double tsmExponent, double tsmFactor,
                          double chlExponent, double chlFactor,
                          ReflectanceEnum inputReflecAre,
                          NNInputMapper invIopMapper, NNInputMapper invKdMapper,
                          ThreadLocal<NNffbpAlphaTabFast> threadLocalForwardIopNet,
                          ThreadLocal<NNffbpAlphaTabFast> threadLocalInverseIopNet,
                          ThreadLocal<NNffbpAlphaTabFast> threadLocalInverseKdNet) {
        this.outputKdSpectrum = outputAllKds;
        this.outputAPoc = outputAPoc;
        this.spectrumOutOfScopeThreshold = spectrumOutOfScopeThreshold;
        this.tsmExponent = tsmExponent;
        this.tsmFactor = tsmFactor;
        this.chlExponent = chlExponent;
        this.chlFactor = chlFactor;
        this.inputReflecAre = inputReflecAre;
        this.invIopMapper = invIopMapper;
        this.invKdMapper = invKdMapper;
        this.threadLocalForwardIopNet = threadLocalForwardIopNet;
        this.threadLocalInverseIopNet = threadLocalInverseIopNet;
        this.threadLocalInverseKdNet = threadLocalInverseKdNet;
    }

    public void perform(double solzen, double satzen, double azi_diff_deg, Sample[] sourceSamples,
                            WritableSample[] targetSamples, double salinity, double temperature) {
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

        /* prepare for water net */
        double[] backwardIOPInput = getBackwardWaterInput(invIopMapper, solzen, satzen, azi_diff_deg, salinity, temperature, RLw);
        NNffbpAlphaTabFast inverseIopNet = threadLocalInverseIopNet.get();
        // test if water leaving radiance reflectance are within training range, otherwise set to training range
        if (isInputInTrainigRange(backwardIOPInput, inverseIopNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(WLR_OOR_BIT_INDEX, true);
        }

        /* calculate concentrations using the water nn */
        double[] backwardWaterOutput = inverseIopNet.calc(backwardIOPInput);
        fillTargetSamplesIOP(backwardWaterOutput, targetSamples);

        /* test if concentrations are within training range */
        if (isWaterConcentrationOOR(backwardWaterOutput, inverseIopNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(CONC_OOR_BIT_INDEX, true);
        }

        /* do forward NN computation */
        double[] forwardWaterInput = getForwardWaterInput(solzen, satzen, azi_diff_deg, salinity, temperature,
                                                          backwardWaterOutput);
        NNffbpAlphaTabFast forwardIopNet = threadLocalForwardIopNet.get();
        double[] forwardWaterOutput = forwardIopNet.calc(forwardWaterInput);

        // new NN from RD, 20130308: we may have now 29 outputs instead of 12, so we need to pick the right ones...
        double[] forwardWaterOutputReduced;
        if (forwardWaterOutput.length == 29) {
            forwardWaterOutputReduced = reduceForwardWaterOutput(forwardWaterOutput);
        } else {
            forwardWaterOutputReduced = forwardWaterOutput;
        }

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = computeChiSquare(forwardWaterOutputReduced, RLw);

        targetSamples[TARGET_CHI_SQUARE_INDEX].set(chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            targetSamples[TARGET_FLAG_INDEX].set(OOTR_BIT_INDEX, true);
        }

        NNffbpAlphaTabFast inverseKdNet = threadLocalInverseKdNet.get();
        double[] backwardKdInput = getBackwardWaterInput(invKdMapper, solzen, satzen, azi_diff_deg, salinity, temperature, RLw);
        double[] backwardKdOutput = inverseKdNet.calc(backwardKdInput);

        // compute k_min and z90_max RD 20060811
        final KMin kMin = createKMin(targetSamples);
        // todo - What shall we use?
        // If we use the k_min computed by the neural net, it won't be consistent with the kd-spectrum
        // If we use the k_min from the class KMin we have a huge difference
//        double k_min = kMin.computeKMinValue();
        double k_min = Math.exp(backwardKdOutput[0]);
        targetSamples[TARGET_K_MIN_INDEX].set(k_min);
        targetSamples[TARGET_Z90_MAX_INDEX].set(-1.0 / k_min);

        if (outputKdSpectrum) {
//            double[] kdSpectrum = kMin.computeKdSpectrum();
//            for (int i = 0; i < kdSpectrum.length; i++) {
//                double aKdSpectrum = kdSpectrum[i];
//                targetSamples[TARGET_KD_SPECTRUM_START_INDEX + i].set(aKdSpectrum);
//            }

            // we have now the Kd spectrum as output from the net (new net 27x41x27_425.4.net, RD 20130131)
            double[] kdSpectrum = backwardKdOutput;
            for (int i = 0; i < kdSpectrum.length; i++) {
                targetSamples[TARGET_KD_SPECTRUM_START_INDEX + i].set(kdSpectrum[i]);
            }
        } else {
            targetSamples[TARGET_KD_490_INDEX].set(kMin.computeKd490());
        }

        final double turbidity = computeTurbidityIndex(Math.log(RLw[5]));// parameter Rlw at 620 'reflec_6'
        targetSamples[TARGET_TURBIDITY_INDEX_INDEX].set(turbidity);
    }

    private double[] reduceForwardWaterOutput(double[] forwardWaterOutput) {
        double[] reducedForwardWaterOutput = new double[12];

        // pick up the right outputs,
        // see old net (17x27x17_487.0.net, 12 outputs) and new net (17x97x47_39.5.net, 29 outputs):
        reducedForwardWaterOutput[0] = forwardWaterOutput[1]; // 412
        reducedForwardWaterOutput[1] = forwardWaterOutput[2]; // 443
        reducedForwardWaterOutput[2] = forwardWaterOutput[4]; // 489
        reducedForwardWaterOutput[3] = forwardWaterOutput[6]; // 510
        reducedForwardWaterOutput[4] = forwardWaterOutput[11]; // 560
        reducedForwardWaterOutput[5] = forwardWaterOutput[12]; // 620
        reducedForwardWaterOutput[6] = forwardWaterOutput[15]; // 665
        reducedForwardWaterOutput[7] = forwardWaterOutput[19]; // 681
        reducedForwardWaterOutput[8] = forwardWaterOutput[20]; // 709
        reducedForwardWaterOutput[9] = forwardWaterOutput[22]; // 754
        reducedForwardWaterOutput[10] = forwardWaterOutput[24]; // 779
        reducedForwardWaterOutput[11] = forwardWaterOutput[27]; // 865

        return reducedForwardWaterOutput;
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

    private double computeChiSquare(double[] forwardWaterOutput, double[] rlw) {
        double chiSquare = 0.0;
        for (int i = 0; i < forwardWaterOutput.length; i++) {
            chiSquare += Math.pow(forwardWaterOutput[i] - rlw[i], 2);
        }
        return chiSquare;
    }

    private double[] getForwardWaterInput(double solzen, double satzen, double azi_diff_deg,
                                          double salinity, double temperature, double[] waterOutnet) {
        double[] forwardWaterInnet = new double[10];
        forwardWaterInnet[0] = solzen;
        forwardWaterInnet[1] = satzen;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = temperature;
        forwardWaterInnet[4] = salinity;
        forwardWaterInnet[5] = waterOutnet[0];
        forwardWaterInnet[6] = waterOutnet[1];
        forwardWaterInnet[7] = waterOutnet[2];
        forwardWaterInnet[8] = waterOutnet[3];       // we assume that bmin is same as bpart (todo: check with RD)
        forwardWaterInnet[9] = waterOutnet[4];
        return forwardWaterInnet;

    }

    private void fillTargetSamplesIOP(double[] backwardWaterOutput, WritableSample[] targetSamples) {
        double aPig = Math.exp(backwardWaterOutput[0]);
        double aDet = Math.exp(backwardWaterOutput[1]);
        double aPart = aDet;
        double aGelbstoff = Math.exp(backwardWaterOutput[2]);

        double chlConc = chlFactor * Math.pow(aPig, chlExponent);
        targetSamples[TARGET_CHL_CONC_INDEX].set(chlConc);

        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(aGelbstoff);
        targetSamples[TARGET_A_DET_INDEX].set(aDet);
        targetSamples[TARGET_A_PIGMENT_INDEX].set(aPig);
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff + aPart);

        double bTsm = Math.exp(backwardWaterOutput[3]);
        double bWhit = Math.exp(backwardWaterOutput[4]);
        targetSamples[TARGET_B_TSM_INDEX].set(bTsm);
        targetSamples[TARGET_B_WHIT_INDEX].set(bWhit);
        targetSamples[TARGET_BB_SPM_INDEX].set(bTsm * BTSM_TO_SPM_FACTOR);
        targetSamples[TARGET_TSM_INDEX].set(tsmFactor * Math.pow(bTsm + bWhit, tsmExponent));

        if (outputAPoc) {
            // todo - How to compute a_poc_443?
            targetSamples[TARGET_A_POC_INDEX].set(0.0);
        }
    }

    private static double[] getBackwardWaterInput(NNInputMapper inputMapper, double solzen, double satzen, double azi_diff_deg, double salinity,
                                           double temperature, double[] rlw) {
        int numReflInputs = inputMapper.getNumInputs();
        int[] mapping = inputMapper.getMapping();
        boolean isLogScaled = inputMapper.isLogScaled();
        double[] waterInnet = new double[5 + numReflInputs];

        waterInnet[0] = solzen;
        waterInnet[1] = satzen;
        waterInnet[2] = azi_diff_deg;
        waterInnet[3] = temperature;
        waterInnet[4] = salinity;

        for (int i = 5; i < waterInnet.length; i++) {
            double v = rlw[mapping[i - 5]];
            if (isLogScaled) {
                v = Math.log(v);
            }
            waterInnet[i] = v;
        }
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
    private static boolean isWaterConcentrationOOR(double[] backwardWaterOutput, NNffbpAlphaTabFast backwardWaterNet) {
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
