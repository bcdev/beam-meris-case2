package org.esa.beam.meris.case2.water;

import org.esa.beam.atmosphere.operator.ReflectanceEnum;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.meris.case2.NeuralNetReader;
import org.esa.beam.meris.case2.algorithm.KMin;
import org.esa.beam.nn.NNffbpAlphaTabFast;

import java.io.IOException;

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
    public static final int SOURCE_REFLEC_10_INDEX = 9;
    public static final int SOURCE_REFLEC_12_INDEX = 10;
    public static final int SOURCE_SOLAZI_INDEX = 11;
    public static final int SOURCE_SOLZEN_INDEX = 12;
    public static final int SOURCE_SATAZI_INDEX = 13;
    public static final int SOURCE_SATZEN_INDEX = 14;
    public static final int SOURCE_ZONAL_WIND_INDEX = 15;
    public static final int SOURCE_MERID_WIND_INDEX = 16;

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
    private ThreadLocal<NNffbpAlphaTabFast> chlRatNet;
    private ThreadLocal<NNffbpAlphaTabFast> ysRatNet;
    private ThreadLocal<NNffbpAlphaTabFast> bbRatNet;
    private ThreadLocal<NNffbpAlphaTabFast> kdminRatNet;
    private ThreadLocal<NNffbpAlphaTabFast> twoFlowNet;

    protected WaterAlgorithm(double spectrumOutOfScopeThreshold) {
        this.spectrumOutOfScopeThreshold = spectrumOutOfScopeThreshold;
        final String chlRatNetString = NeuralNetReader.readNeuralNetString(
                "/org/esa/beam/meris/case2/regional/chl_rat560/2_16.2.net", null);

        chlRatNet = new ThreadLocal<NNffbpAlphaTabFast>() {
            @Override
            protected NNffbpAlphaTabFast initialValue() {
                try {
                    return new NNffbpAlphaTabFast(chlRatNetString);
                } catch (IOException e) {
                    throw new OperatorException("Not able to init neural net", e);
                }
            }
        };

        final String ysRatNetString = NeuralNetReader.readNeuralNetString(
                "/org/esa/beam/meris/case2/regional/ag_rat560/1_5.0.net", null);

        ysRatNet = new ThreadLocal<NNffbpAlphaTabFast>() {
            @Override
            protected NNffbpAlphaTabFast initialValue() {
                try {
                    return new NNffbpAlphaTabFast(ysRatNetString);
                } catch (IOException e) {
                    throw new OperatorException("Not able to init neural net", e);
                }
            }
        };
        final String bbRatNetString = NeuralNetReader.readNeuralNetString(
                "/org/esa/beam/meris/case2/regional/bb_rat/1_6.2.net", null);

        bbRatNet = new ThreadLocal<NNffbpAlphaTabFast>() {
            @Override
            protected NNffbpAlphaTabFast initialValue() {
                try {
                    return new NNffbpAlphaTabFast(bbRatNetString);
                } catch (IOException e) {
                    throw new OperatorException("Not able to init neural net", e);
                }
            }
        };
        final String kdminNetString = NeuralNetReader.readNeuralNetString(
                "/org/esa/beam/meris/case2/regional/kdmin/1_4.1.net", null);

        kdminRatNet = new ThreadLocal<NNffbpAlphaTabFast>() {
            @Override
            protected NNffbpAlphaTabFast initialValue() {
                try {
                    return new NNffbpAlphaTabFast(kdminNetString);
                } catch (IOException e) {
                    throw new OperatorException("Not able to init neural net", e);
                }
            }
        };

        // Deactivated all inputs are out of training range
//        final String twoFlowNetString = NeuralNetReader.readNeuralNetString(
//                "/org/esa/beam/meris/case2/regional/2flow_dif4/7x5_103.5.net", null);
//
//        twoFlowNet = new ThreadLocal<NNffbpAlphaTabFast>() {
//            @Override
//            protected NNffbpAlphaTabFast initialValue() {
//                try {
//                    return new NNffbpAlphaTabFast(twoFlowNetString);
//                } catch (IOException e) {
//                    throw new OperatorException("Not able to init neural net", e);
//                }
//            }
//        };

    }

    public double[] perform(NNffbpAlphaTabFast inverseWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                            double solzen, double satzen, double azi_diff_deg, Sample[] sourceSamples,
                            WritableSample[] targetSamples, ReflectanceEnum inputReflecAre) {
        /* determine cut_thresh from waterNet minimum */
        double cut_thresh = getCutThreshold(inverseWaterNet.getInmin());

        // test RLw against lowest or cut value in NN and set in lower
        double[] RLw = new double[11];
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
        if (ReflectanceEnum.IRRADIANCE_REFLECTANCES.equals(inputReflecAre)) {
            for (int i = 0; i < RLw.length; i++) {
                RLw[i] /= Math.PI;
            }
        }
        double[] RLw_cut = new double[RLw.length];
        for (int i = 0; i < RLw.length; i++) {
            final double Rlw = RLw[i];
            if (Rlw < cut_thresh) {
                RLw_cut[i] = cut_thresh;
            } else {
                RLw_cut[i] = Rlw;
            }
        }

        /* prepare for water net */
        double[] waterInnet = getWaterInnet(solzen, satzen, azi_diff_deg, RLw_cut);

        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (!test_logRLw(waterInnet, inverseWaterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(WLR_OOR_BIT_INDEX, true);
        }

        /* calculate concentrations using the water nn */
        double[] waterOutnet = inverseWaterNet.calc(waterInnet);

        fillOutput(waterOutnet, targetSamples);
        /////////////////////////////////////////////////////////////////////////////
        // New nets from 25.05.2011 for CoastColour

        double chl = computeChlWithChlRatNet(RLw, solzen);
        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(chl));

        double ys = computeChlWithYsRatNet(RLw, solzen);
        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(Math.exp(ys));

        double[] bb = computeChlWithBbRatNet(RLw, solzen);
        double logBb560 = bb[0];
        double logBb620 = bb[1];
        targetSamples[TARGET_BB_SPM_INDEX].set(Math.exp(logBb560));

        // TODO add option to select which one to use for Chl computation
        // Deactivated all inputs are out of training range
//        double[] twoFlowResult = computeChlWithTwoFlowNet(RLw);
//        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(twoFlowResult[0]));
//        targetSamples[TARGET_A_PIGMENT_INDEX].set(Math.exp(twoFlowResult[1]));
        // New nets from 25.05.2011 for CoastColour
        /////////////////////////////////////////////////////////////////////////////

        /* test if concentrations are within training range */
        final double bbSpm = targetSamples[TARGET_BB_SPM_INDEX].getDouble();
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        if (!test_watconc(bbSpm / BTSM_TO_SPM_FACTOR, aPig, aGelbstoff, inverseWaterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(CONC_OOR_BIT_INDEX, true);
        }

        /* do forward NN computation */
        double[] forwardWaterInnet = getForwardWaterInnet(solzen, satzen, azi_diff_deg, waterOutnet);
        double[] forwardWaterOutnet = forwardWaterNet.calc(forwardWaterInnet);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = computeChiSquare(forwardWaterOutnet, RLw_cut);

        targetSamples[TARGET_CHI_SQUARE_INDEX].set(chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            targetSamples[TARGET_FLAG_INDEX].set(OOTR_BIT_INDEX, true);
        }
        // compute k_min and z90_max RD 20060811
        final KMin kMin = createKMin(targetSamples);

//        double k_min = kMin.computeKMinValue();
        double k_min = Math.exp(computeKminWithKdminNet(RLw, solzen));
        targetSamples[TARGET_K_MIN_INDEX].set(k_min);
        targetSamples[TARGET_Z90_MAX_INDEX].set(-1.0 / k_min);

        targetSamples[TARGET_KD_490_INDEX].set(kMin.computeKd490());

        final double turbidity = computeTurbidityIndex(RLw[5]);// parameter Rlw at 620 'reflec_6'
        targetSamples[TARGET_TURBIDITY_INDEX_INDEX].set(turbidity);
        return RLw_cut;

    }

    private double computeKminWithKdminNet(double[] rLw, double solzen) {
        double[] inValues = new double[6];
        inValues[0] = solzen;
        inValues[1] = Math.log(rLw[1] / rLw[0]);
        inValues[2] = Math.log(rLw[2] / rLw[1]);
        inValues[3] = Math.log(rLw[3] / rLw[2]);
        inValues[4] = Math.log(rLw[4] / rLw[3]);
        inValues[5] = Math.log(rLw[6] / rLw[4]);

        return kdminRatNet.get().calc(inValues)[0];

    }

    private double[] computeChlWithBbRatNet(double[] rLw, double solzen) {
        double[] inValues = new double[6];
        inValues[0] = solzen;
        inValues[1] = Math.log(rLw[1] / rLw[0]);
        inValues[2] = Math.log(rLw[2] / rLw[1]);
        inValues[3] = Math.log(rLw[3] / rLw[2]);
        inValues[4] = Math.log(rLw[4] / rLw[3]);
        inValues[5] = Math.log(rLw[6] / rLw[4]);

        return bbRatNet.get().calc(inValues);
    }

    private double computeChlWithYsRatNet(double[] rLw, double solzen) {
        double[] inValues = new double[6];
        inValues[0] = solzen;
        inValues[1] = Math.log(rLw[0] / rLw[4]);
        inValues[2] = Math.log(rLw[1] / rLw[4]);
        inValues[3] = Math.log(rLw[2] / rLw[4]);
        inValues[4] = Math.log(rLw[3] / rLw[4]);
        inValues[5] = Math.log(rLw[6] / rLw[4]);

        return ysRatNet.get().calc(inValues)[0];
    }

    protected double computeChlWithChlRatNet(double[] rLw, double solzen) {
        double[] inValues = new double[6];
        inValues[0] = solzen;
        inValues[1] = Math.log(rLw[0] / rLw[4]);
        inValues[2] = Math.log(rLw[1] / rLw[4]);
        inValues[3] = Math.log(rLw[2] / rLw[4]);
        inValues[4] = Math.log(rLw[3] / rLw[4]);
        inValues[5] = Math.log(rLw[6] / rLw[4]);

        return chlRatNet.get().calc(inValues)[0];
    }

    private double[] computeChlWithTwoFlowNet(double[] rLw) {
        double[] inValues = new double[4];
        inValues[0] = rLw[5] - rLw[5 + 1];  // 620 - 665
        inValues[1] = rLw[6] - rLw[6 + 1];  // 665 - 681
        inValues[2] = rLw[8] - rLw[8 + 1];  // 708 - 753
        inValues[3] = rLw[9] - rLw[9 + 1];  // 753 - 778
        // two values are returned; log_conc_chl and log_apig2
        return twoFlowNet.get().calc(inValues);
    }


    private double computeTurbidityIndex(double rlw620) {
        if (rlw620 > RLW620_MAX) {  // maximum value for computing the turbidity Index
            rlw620 = RLW620_MAX;
        }
        double rho = rlw620 * Math.PI;
        return TURBIDITY_AT * rho / (1 - rho / TURBIDITY_C) + TURBIDITY_BT;
    }


    protected abstract KMin createKMin(WritableSample[] targetSamples);

    protected abstract double computeChiSquare(double[] forwardWaterOutnet, double[] RLw_cut);

    protected abstract double[] getForwardWaterInnet(double solzen, double satzen, double azi_diff_deg,
                                                     double[] waterOutnet);

    protected abstract void fillOutput(double[] waterOutnet, WritableSample[] targetSamples);

    protected abstract double[] getWaterInnet(double teta_sun_deg, double teta_view_deg, double azi_diff_deg,
                                              double[] RLw_cut);

    protected abstract double getCutThreshold(double[] inmin);

    /*-----------------------------------------------------------------------------------
     **	test water leaving radiances as input to neural network for out of training range
     **	if out of range set to lower or upper boundary value
    -----------------------------------------------------------------------------------*/
    private boolean test_logRLw(double[] innet, NNffbpAlphaTabFast inverseWaterNet) {
        final double[] inmax = inverseWaterNet.getInmax();
        final double[] inmin = inverseWaterNet.getInmin();
        for (int i = 0; i < innet.length; i++) {
            if (innet[i] > inmax[i]) {
                innet[i] = inmax[i];
                return false;
            }
            if (innet[i] < inmin[i]) {
                innet[i] = inmin[i];
                return false;
            }
        }
        return true;
    }

    /*-------------------------------------------------------------------------------
     **	test water constituents as output of neural network for out of training range
     **
    --------------------------------------------------------------------------------*/
    private boolean test_watconc(double bTsm, double aPig, double aGelbstoff, NNffbpAlphaTabFast inverseWaterNet) {
        double log_spm = Math.log(bTsm);
        double log_pig = Math.log(aPig);
        double log_gelb = Math.log(aGelbstoff);
        final double[] outmax = inverseWaterNet.getOutmax();
        final double[] outmin = inverseWaterNet.getOutmin();
        final boolean ootr0 = log_spm > outmax[0] || log_spm < outmin[0];
        final boolean ootr1 = log_pig > outmax[1] || log_pig < outmin[1];
        final boolean ootr2 = log_gelb > outmax[2] || log_gelb < outmin[2];
        return !(ootr0 || ootr1 || ootr2);
    }
}
