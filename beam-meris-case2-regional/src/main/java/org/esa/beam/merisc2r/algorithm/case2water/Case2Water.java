package org.esa.beam.merisc2r.algorithm.case2water;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.KMin;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;


public class Case2Water {

    private NNffbpAlphaTabFast inverseWaterNet;
    private NNffbpAlphaTabFast forwardWaterNet;

    private double tsmExponent;
    private double tsmFactor;
    private double chlExponent;
    private double chlFactor;

    private double spectrumOutOfScopeThreshold;

    public void init(NNffbpAlphaTabFast inverseWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                     AlgorithmParameter parameter) {
        this.inverseWaterNet = inverseWaterNet;
        this.forwardWaterNet = forwardWaterNet;
        tsmExponent = parameter.tsmConversionExponent;
        tsmFactor = parameter.tsmConversionFactor;
        chlExponent = parameter.chlConversionExponent;
        chlFactor = parameter.chlConversionFactor;
        this.spectrumOutOfScopeThreshold = parameter.spectrumOutOfScopeThreshold;

    }


    public double[] perform(double teta_sun_deg,
                            double teta_view_deg, double azi_diff_deg,
                            OutputBands outputBands) {

        /* determine cut_thresh from waterNet minimum */
        double cut_thresh = 1000.0;
        final double[] inmin = inverseWaterNet.getInmin();
        for (int i = 3; i < 11; i++) {
            double inmini = Math.exp(inmin[i]);
            if (inmini < cut_thresh) {
                cut_thresh = inmini;
            }
        }

        // test RLw against lowest or cut value in NN and set in lower
        // start with  MERIS band 2 443
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
        double[] waterInnet = new double[11];
        waterInnet[0] = teta_sun_deg;
        waterInnet[1] = teta_view_deg;
        waterInnet[2] = azi_diff_deg;

        for (int i = 3; i < 10; i++) {
            waterInnet[i] = Math.log(RLw_cut[i - 3]); /* bands 1-7 == 412 - 664 nm */
        }
        waterInnet[10] = Math.log(RLw_cut[8]); /* band 708 nm */


        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (!test_logRLw(waterInnet, inverseWaterNet)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.WLR_OOR);
        }

        /* calculate concentrations using the water nn */
        double[] waterOutnet = inverseWaterNet.calc(waterInnet);

        double bTsm = Math.exp(waterOutnet[0]);
        outputBands.setValue("b_tsm", bTsm);
        outputBands.setValue("tsm", Math.exp(Math.log(tsmFactor) + waterOutnet[0] * tsmExponent));

        double aPig = Math.exp(waterOutnet[1]);
        outputBands.setValue("a_pig", aPig);
        outputBands.setValue("chl_conc", Math.exp(Math.log(chlFactor) + waterOutnet[1] * chlExponent));

        double aGelbstoff = Math.exp(waterOutnet[2]);
        outputBands.setValue("a_gelbstoff", aGelbstoff);
        outputBands.setValue("a_total", aPig + aGelbstoff);

        /* test if concentrations are within training range */
        if (!test_watconc(bTsm, aPig, aGelbstoff, inverseWaterNet)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.CONC_OOR);
        }

        /* do forward NN computation */
        double[] forwardWaterInnet = new double[6];
        forwardWaterInnet[0] = teta_sun_deg;
        forwardWaterInnet[1] = teta_view_deg;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log gelbstoff
        forwardWaterInnet[4] = waterOutnet[1]; // log pigment
        forwardWaterInnet[5] = waterOutnet[2]; // log tsm
        double[] forwardWaterOutnet = forwardWaterNet.calc(forwardWaterInnet);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = Math.pow(forwardWaterOutnet[0] - Math.log(RLw_cut[0]), 2) +
                           Math.pow(forwardWaterOutnet[1] - Math.log(RLw_cut[1]), 2) +
                           Math.pow(forwardWaterOutnet[2] - Math.log(RLw_cut[2]), 2) +
                           Math.pow(forwardWaterOutnet[3] - Math.log(RLw_cut[3]), 2) +
                           Math.pow(forwardWaterOutnet[4] - Math.log(RLw_cut[4]), 2) +
                           Math.pow(forwardWaterOutnet[5] - Math.log(RLw_cut[5]), 2) +
                           Math.pow(forwardWaterOutnet[6] - Math.log(RLw_cut[6]), 2) +
                           Math.pow(forwardWaterOutnet[7] - Math.log(RLw_cut[8]),
                                    2); /* in outnet 7 corresponds to RLw 8 */

        outputBands.setValue("chiSquare", chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.OOTR);
        }
        // compute k_min and z90_max RD 20060811
        final KMin kMin = new KMin();
        double k_min = kMin.perform(bTsm, aPig, aGelbstoff);
        outputBands.setValue("K_min", k_min);
        outputBands.setValue("Z90_max", -1.0 / k_min);
        return RLw_cut;
    }

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
