package org.esa.beam.meris.case2.fit;

import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.nn.NNffbpAlphaTabFast;

import static org.esa.beam.meris.case2.water.WaterAlgorithm.*;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class ChiSquareFitting {

    private double tsmExponent;
    private double tsmFactor;
    private double chlExponent;
    private double chlFactor;
    private MerisGLM myFitLvMq;
    private Data4SingleFitInitialization initSingleFit;
    private double fitFailedThreshold;

    public ChiSquareFitting(double tsmConversionExponent, double tsmConversionFactor, double chlConversionExponent,
                            double chlConversionFactor, MerisGLM glm) {
        tsmExponent = tsmConversionExponent;
        tsmFactor = tsmConversionFactor;
        chlExponent = chlConversionExponent;
        chlFactor = chlConversionFactor;
        myFitLvMq = glm;
        fitFailedThreshold = 14.0;
        initSingleFit = new Data4SingleFitInitialization();
        // it's suffiecient to initialize these constant values only once
        initSingleFit.ln_a_Chlor = -2.5;
        initSingleFit.ln_a_Yellow_a_SPM = -2.0;
        initSingleFit.ln_b_SPM_b_White = 0.0;
    }

    public void perform(NNffbpAlphaTabFast forwardWaterNet, double[] RLw_cut,
                        double teta_sun_deg, double teta_view_deg, double azi_diff_deg,
                        WritableSample[] targetSamples) {

        final double waterReflLogVariance = 1.5;
        myFitLvMq.initSetOfFits(forwardWaterNet, waterReflLogVariance);

        initSingleFit.theta_sun_grad = teta_sun_deg;
        initSingleFit.theta_view_grad = teta_view_deg;
        initSingleFit.azi_diff_grad = azi_diff_deg;
        initSingleFit.ln_a_Chlor = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        initSingleFit.ln_a_Yellow_a_SPM = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        initSingleFit.ln_b_SPM_b_White = targetSamples[TARGET_BB_SPM_INDEX].getDouble();

        for (int k = 0; k < 7; k++) {
            initSingleFit.wlRefl[k] = Math.log(RLw_cut[k]);
        }
        initSingleFit.wlRefl[7] = Math.log(RLw_cut[8]);

        myFitLvMq.initSingleFit(initSingleFit);
        FitResult fitRes = myFitLvMq.getMyLM().LMFit();

        targetSamples[TARGET_TSM_FIT_INDEX].set(Math.exp(Math.log(tsmFactor) + fitRes.parsfit[0] * tsmExponent));
        targetSamples[TARGET_CHL_CONC_FIT_INDEX].set(Math.exp(Math.log(chlFactor) + fitRes.parsfit[1] * chlExponent));

        final double[] inmax = forwardWaterNet.getInmax();
        final double[] inmin = forwardWaterNet.getInmin();

        double deltaBtsm = getDelta(fitRes.CovPars.get(0, 0));
        final double bTsmMax = Math.exp(getMax(fitRes.parsfit[0], deltaBtsm, inmax[3]));
        final double bTsmMin = Math.exp(getMin(fitRes.parsfit[0], deltaBtsm, inmin[3]));
        targetSamples[TARGET_B_TSM_FIT_INDEX].set(Math.exp(fitRes.parsfit[0]));
        targetSamples[TARGET_B_TSM_FIT_MAX_INDEX].set(bTsmMax);
        targetSamples[TARGET_B_TSM_FIT_MIN_INDEX].set(bTsmMin);

        double deltaApig = getDelta(fitRes.CovPars.get(1, 1));
        double apigMax = Math.exp(getMax(fitRes.parsfit[1], deltaApig, inmax[4]));
        double apigMin = Math.exp(getMin(fitRes.parsfit[1], deltaApig, inmin[4]));
        targetSamples[TARGET_A_PIG_FIT_INDEX].set(Math.exp(fitRes.parsfit[1]));
        targetSamples[TARGET_A_PIG_FIT_MAX_INDEX].set(apigMax);
        targetSamples[TARGET_A_PIG_FIT_MIN_INDEX].set(apigMin);

        double deltaGelbstoff = getDelta(fitRes.CovPars.get(2, 2));
        double gelbstoffMax = Math.exp(getMax(fitRes.parsfit[2], deltaGelbstoff, inmax[5]));
        double gelbstoffMin = Math.exp(getMin(fitRes.parsfit[2], deltaGelbstoff, inmin[5]));
        targetSamples[TARGET_A_GELBSTOFF_FIT_INDEX].set(Math.exp(fitRes.parsfit[2]));
        targetSamples[TARGET_A_GELBSTOFF_FIT_MAX_INDEX].set(gelbstoffMax);
        targetSamples[TARGET_A_GELBSTOFF_FIT_MIN_INDEX].set(gelbstoffMin);

        targetSamples[TARGET_CHI_SQUARE_FIT_INDEX].set(fitRes.ChiSq);
        if (fitRes.ChiSq > fitFailedThreshold) {
            targetSamples[TARGET_FLAG_INDEX].set(FIT_FAILED_INDEX, true);
        }

        targetSamples[TARGET_N_ITER_FIT_INDEX].set(fitRes.niter);
        targetSamples[TARGET_PARAM_CHANGE_FIT_INDEX].set(fitRes.startChiSq);
    }

    private double getMax(double value, double delta, double absMax) {
        double actualMax = value + delta;
        if (actualMax > absMax) {
            actualMax = absMax;
        }
        return actualMax;
    }

    private double getMin(double value, double delta, double absMin) {
        double actualMin = value - delta;
        if (actualMin < absMin) {
            actualMin = absMin;
        }
        return actualMin;
    }

    private double getDelta(double covValue) {
        double delta;
        if (covValue < 0 || Double.isNaN(covValue)) {
            delta = Math.sqrt(1000);
        } else {
            delta = Math.sqrt(covValue);
        }

        return delta;
    }


    public static class Data4SingleFitInitialization {

        public double theta_sun_grad;
        public double theta_view_grad;
        public double azi_diff_grad;
        public double ln_a_Chlor;
        public double ln_b_SPM_b_White;
        public double ln_a_Yellow_a_SPM;
        public double[] wlRefl = new double[8];
    }
}
