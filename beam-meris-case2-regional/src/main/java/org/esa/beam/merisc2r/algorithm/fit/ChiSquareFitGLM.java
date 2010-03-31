package org.esa.beam.merisc2r.algorithm.fit;

import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;
import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.fit.ChiSquareFit;
import org.esa.beam.case2.algorithm.Auxdata;

public class ChiSquareFitGLM implements ChiSquareFit {
    private double tsmExponent;
    private double tsmFactor;
    private double chlExponent;
    private double chlFactor;
    private MerisC2R_GLM myFitLvMq;
    private Data4SingleFitInitialization initSingleFit;
    private NNffbpAlphaTabFast forwardWaterNet;
    private AlgorithmParameter parameter;


    public void init(AlgorithmParameter parameter, Auxdata auxdata) {
        this.parameter = parameter;
        tsmExponent = parameter.tsmConversionExponent;
        tsmFactor = parameter.tsmConversionFactor;
        chlExponent = parameter.chlConversionExponent;
        chlFactor = parameter.chlConversionFactor;
        forwardWaterNet = auxdata.getForwardWaterNet();
        myFitLvMq = new MerisC2R_GLM();
        myFitLvMq.initSetOfFits(forwardWaterNet, parameter.waterReflLogVariance);
        initSingleFit = new Data4SingleFitInitialization();
        // it's suffiecient to initialize these constant values only once
        initSingleFit.ln_a_Chlor = -2.5;
        initSingleFit.ln_a_Yellow_a_SPM = -2.0;
        initSingleFit.ln_b_SPM_b_White = 0.0;
    }

    public void perform(double teta_sun_deg, double teta_view_deg, double azi_diff_deg, double[] RLw_cut,
                        OutputBands outputBands) {
        initSingleFit.theta_sun_grad = teta_sun_deg;
        initSingleFit.theta_view_grad = teta_view_deg;
        initSingleFit.azi_diff_grad = azi_diff_deg;
        initSingleFit.ln_a_Chlor = outputBands.getDoubleValue("a_pig");
        initSingleFit.ln_a_Yellow_a_SPM = outputBands.getDoubleValue("a_gelbstoff");
        initSingleFit.ln_b_SPM_b_White = outputBands.getDoubleValue("b_tsm");

        // todo - check this loop compared to the on in the Boreal processor
        for (int k = 0; k < 7; k++) {
            initSingleFit.wlRefl[k] = Math.log(RLw_cut[k]);
        }
        initSingleFit.wlRefl[7] = Math.log(RLw_cut[8]);

        myFitLvMq.initSingleFit(initSingleFit);
        FitResult fitRes = myFitLvMq.myLM.LMFit();

        outputBands.setValue("tsmFit", Math.exp(Math.log(tsmFactor) + fitRes.parsfit[0] * tsmExponent));
        outputBands.setValue("chl_concFit", Math.exp(Math.log(chlFactor) + fitRes.parsfit[1] * chlExponent));

        outputBands.setValue("b_tsmFit", Math.exp(fitRes.parsfit[0]));
        double deltaBtsm = getDelta(fitRes.CovPars.get(0, 0));
        double btsmMax = Math.exp(getMax(fitRes.parsfit[0], deltaBtsm, forwardWaterNet.inmax[3]));
        outputBands.setValue("b_tsmFit_max", btsmMax);
        double btsmMin = Math.exp(getMin(fitRes.parsfit[0], deltaBtsm, forwardWaterNet.inmin[3]));
        outputBands.setValue("b_tsmFit_min", btsmMin);

        outputBands.setValue("a_pigFit", Math.exp(fitRes.parsfit[1]));
        double deltaApig = getDelta(fitRes.CovPars.get(1, 1));
        double apigMax = Math.exp(getMax(fitRes.parsfit[1], deltaApig, forwardWaterNet.inmax[4]));
        outputBands.setValue("a_pigFit_max", apigMax);
        double apigMin = Math.exp(getMin(fitRes.parsfit[1], deltaApig,forwardWaterNet.inmin[4]));
        outputBands.setValue("a_pigFit_min", apigMin);

        outputBands.setValue("a_gelbstoffFit", Math.exp(fitRes.parsfit[2]));
        double deltaGelbstoff = getDelta(fitRes.CovPars.get(2, 2));
        double gelbstoffMax = Math.exp(getMax(fitRes.parsfit[2], deltaGelbstoff, forwardWaterNet.inmax[5]));
        outputBands.setValue("a_gelbstoffFit_max", gelbstoffMax);
        double gelbstoffMin = Math.exp(getMin(fitRes.parsfit[2], deltaGelbstoff, forwardWaterNet.inmin[5]));
        outputBands.setValue("a_gelbstoffFit_min", gelbstoffMin);


        outputBands.setValue("chiSquareFit", fitRes.ChiSq);
        if(fitRes.ChiSq > parameter.fitFailedThreshold) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.FIT_FAILED);
        }

        outputBands.setValue("nIter", fitRes.startChiSq);
        outputBands.setValue("paramChange", fitRes.niter);
    }

    private double getMax(double value, double delta, double absMax) {
        double actualMax = value + delta;
        if(actualMax > absMax) {
            actualMax = absMax;
        }
        return actualMax;
    }

    private double getMin(double value, double delta, double absMin) {
        double actualMin = value - delta;
        if(actualMin < absMin) {
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



    public static class Data4SingleFitInitialization{
        public double theta_sun_grad;
        public double theta_view_grad;
        public double azi_diff_grad;
        public double ln_a_Chlor;
        public double ln_b_SPM_b_White;
        public double ln_a_Yellow_a_SPM;
        public double[] wlRefl = new double[8];
    }
}
