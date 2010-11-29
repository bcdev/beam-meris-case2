package org.esa.beam.meris.case2.water;

import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;
import org.esa.beam.framework.gpf.experimental.PointOperator;

public abstract class WaterAlgorithm {

    public static final int SOURCE_REFLEC_1_INDEX = 0;
    public static final int SOURCE_REFLEC_2_INDEX = 1;
    public static final int SOURCE_REFLEC_3_INDEX = 2;
    public static final int SOURCE_REFLEC_4_INDEX = 3;
    public static final int SOURCE_REFLEC_5_INDEX = 4;
    public static final int SOURCE_REFLEC_6_INDEX = 5;
    public static final int SOURCE_REFLEC_7_INDEX = 6;
    public static final int SOURCE_REFLEC_9_INDEX = 7;
    public static final int SOURCE_REFLEC_10_INDEX = 8;
    public static final int SOURCE_REFLEC_12_INDEX = 9;
    public static final int SOURCE_REFLEC_13_INDEX = 10;
    public static final int SOURCE_SOLAZI_INDEX = 11;
    public static final int SOURCE_SOLZEN_INDEX = 12;
    public static final int SOURCE_SATAZI_INDEX = 13;
    public static final int SOURCE_SATZEN_INDEX = 14;
    public static final int SOURCE_ZONAL_WIND_INDEX = 15;
    public static final int SOURCE_MERID_WIND_INDEX = 16;

    public static final int TARGET_A_GELBSTOFF_INDEX = 0;
    public static final int TARGET_A_PIGMENT_INDEX = 1;
    public static final int TARGET_A_TOTAL_INDEX = 2;
    public static final int TARGET_B_TSM_INDEX = 3;
    public static final int TARGET_TSM_INDEX = 4;
    public static final int TARGET_CHL_CONC_INDEX = 5;
    public static final int TARGET_CHI_SQUARE_INDEX = 6;
    public static final int TARGET_K_MIN_INDEX = 7;
    public static final int TARGET_Z90_MAX_INDEX = 8;
    public static final int TARGET_FLAG_INDEX = 9;
    public static final int TARGET_A_GELBSTOFF_FIT_INDEX = 10;
    public static final int TARGET_A_GELBSTOFF_FIT_MAX_INDEX = 11;
    public static final int TARGET_A_GELBSTOFF_FIT_MIN_INDEX = 12;
    public static final int TARGET_A_PIG_FIT_INDEX = 13;
    public static final int TARGET_A_PIG_FIT_MAX_INDEX = 14;
    public static final int TARGET_A_PIG_FIT_MIN_INDEX = 15;
    public static final int TARGET_B_TSM_FIT_INDEX = 16;
    public static final int TARGET_B_TSM_FIT_MAX_INDEX = 17;
    public static final int TARGET_B_TSM_FIT_MIN_INDEX = 18;
    public static final int TARGET_TSM_FIT_INDEX = 19;
    public static final int TARGET_CHL_CONC_FIT_INDEX = 20;
    public static final int TARGET_CHI_SQUARE_FIT_INDEX = 21;
    public static final int TARGET_N_ITER_FIT_INDEX = 22;
    public static final int TARGET_PARAM_CHANGE_FIT_INDEX = 23;

    public static final int WLR_OOR_BIT_INDEX = 0;
    public static final int CONC_OOR_BIT_INDEX = 1;
    public static final int OOTR_BIT_INDEX = 2;
    public static final int WHITECAPS_BIT_INDEX = 3;
    public static final int FIT_FAILED_INDEX = 4;
    public static final int INVALID_BIT_INDEX = 7;

    private double spectrumOutOfScopeThreshold;

    protected WaterAlgorithm(double spectrumOutOfScopeThreshold) {
        this.spectrumOutOfScopeThreshold = spectrumOutOfScopeThreshold;
    }

    public double[] perform(NNffbpAlphaTabFast inverseWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                            double solzen, double satzen, double azi_diff_deg, PointOperator.Sample[] sourceSamples,
                            PointOperator.WritableSample[] targetSamples) {
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
        RLw[7] = sourceSamples[SOURCE_REFLEC_9_INDEX].getDouble();
        RLw[8] = sourceSamples[SOURCE_REFLEC_10_INDEX].getDouble();
        RLw[9] = sourceSamples[SOURCE_REFLEC_12_INDEX].getDouble();
        RLw[10] = sourceSamples[SOURCE_REFLEC_13_INDEX].getDouble();
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

        /* test if concentrations are within training range */
        final double bTsm = targetSamples[TARGET_B_TSM_INDEX].getDouble();
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        if (!test_watconc(bTsm, aPig, aGelbstoff, inverseWaterNet)) {
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
        double k_min = computeKMin(targetSamples);
        targetSamples[TARGET_K_MIN_INDEX].set(k_min);
        targetSamples[TARGET_Z90_MAX_INDEX].set(-1.0 / k_min);
        return RLw_cut;

    }


    protected abstract double computeKMin(PointOperator.WritableSample[] targetSamples);

    protected abstract double computeChiSquare(double[] forwardWaterOutnet, double[] RLw_cut);

    protected abstract double[] getForwardWaterInnet(double solzen, double satzen, double azi_diff_deg,
                                                     double[] waterOutnet);

    protected abstract void fillOutput(double[] waterOutnet, PointOperator.WritableSample[] targetSamples);

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
