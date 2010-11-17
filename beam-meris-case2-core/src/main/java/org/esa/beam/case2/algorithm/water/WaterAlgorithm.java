package org.esa.beam.case2.algorithm.water;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;

public abstract class WaterAlgorithm {

    private NNffbpAlphaTabFast inverseWaterNet;
    private NNffbpAlphaTabFast forwardWaterNet;
    private AlgorithmParameter parameter;

    public void init(NNffbpAlphaTabFast inverseWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                     AlgorithmParameter parameter) {
        this.inverseWaterNet = inverseWaterNet;
        this.forwardWaterNet = forwardWaterNet;
        this.parameter = parameter;
    }

    public double[] perform(double teta_sun_deg,
                            double teta_view_deg, double azi_diff_deg,
                            OutputBands outputBands) {

        /* determine cut_thresh from waterNet minimum */
        double cut_thresh = getCutThreshold(inverseWaterNet.getInmin());

        // test RLw against lowest or cut value in NN and set in lower
        double[] RLw = outputBands.getDoubleValues("reflec_");
        double[] RLw_cut = new double[RLw.length];
        for (int i = 0; i < RLw.length; i++) {
            if (RLw[i] < cut_thresh) {
                RLw_cut[i] = cut_thresh;
            } else {
                RLw_cut[i] = RLw[i];
            }
        }

        /* prepare for water net */
        double[] waterInnet = getWaterInnet(teta_sun_deg, teta_view_deg, azi_diff_deg, RLw_cut);

        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (!test_logRLw(waterInnet, inverseWaterNet)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.WLR_OOR);
        }

        /* calculate concentrations using the water nn */
        double[] waterOutnet = inverseWaterNet.calc(waterInnet);

        fillOutput(waterOutnet, outputBands);

        /* test if concentrations are within training range */
        if (!test_watconc(outputBands, inverseWaterNet)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.CONC_OOR);
        }

        /* do forward NN computation */
        double[] forwardWaterInnet = getForwardWaterInnet(teta_sun_deg, teta_view_deg, azi_diff_deg, waterOutnet);
        double[] forwardWaterOutnet = forwardWaterNet.calc(forwardWaterInnet);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = computeChiSquare(forwardWaterOutnet, RLw_cut);

        outputBands.setValue("chiSquare", chiSquare);

        if (chiSquare > parameter.spectrumOutOfScopeThreshold) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.OOTR);
        }
        // compute k_min and z90_max RD 20060811
        double k_min = computeKMin(outputBands);
        outputBands.setValue("K_min", k_min);
        outputBands.setValue("Z90_max", -1.0 / k_min);
        return RLw_cut;
    }

    protected abstract double computeKMin(OutputBands outputBands);

    protected abstract double computeChiSquare(double[] forwardWaterOutnet, double[] RLw_cut);

    protected abstract double[] getForwardWaterInnet(double teta_sun_deg, double teta_view_deg, double azi_diff_deg,
                                                     double[] waterOutnet);

    protected abstract void fillOutput(double[] waterOutnet, OutputBands outputBands);

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
    private boolean test_watconc(OutputBands outputBands, NNffbpAlphaTabFast inverseWaterNet) {
        double log_spm = Math.log(outputBands.getDoubleValue("b_tsm"));
        double log_pig = Math.log(outputBands.getDoubleValue("a_pig"));
        double log_gelb = Math.log(outputBands.getDoubleValue("a_gelbstoff"));
        final double[] outmax = inverseWaterNet.getOutmax();
        final double[] outmin = inverseWaterNet.getOutmin();
        final boolean ootr0 = log_spm > outmax[0] || log_spm < outmin[0];
        final boolean ootr1 = log_pig > outmax[1] || log_pig < outmin[1];
        final boolean ootr2 = log_gelb > outmax[2] || log_gelb < outmin[2];
        return !(ootr0 || ootr1 || ootr2);
    }
}
