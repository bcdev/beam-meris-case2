package org.esa.beam.lakes.boreal.algorithm.case2water;

import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;

public class BorealWater {
    private NNffbpAlphaTabFast waterNet;
    private NNffbpAlphaTabFast forwardWaterNet;

    private double spectrumOutOfScopeThreshold;

    public static final double[] fin_pig_a={    //13 bands
//        0.0354, 0.04, 0.022, 0.0153,
//        0.0078, 0.0075, 0.0091, 0.0141,
//        0.0176, 0.0019, 0.000024, 0.00001,
//        0.000001
        0.0385,0.0405,0.0242,0.01651,0.0078,0.007,0.0076,
        0.0145,0.0183,0.00181,0.0002,0.00001,0.000001
    };
    public static final double[] fin_pig_b={    //13 bands
//        0.1397, 0.1496, 0.0944, 0.0576,
//        0.0472, 0.045, 0.0729, 0.1134,
//        0.126, 0.003, 0.0, 0.0, 0.0
        0.2289,0.223,0.2033,0.1703,0.170,0.1423,
        0.077,0.142,0.153,0.0,0.0,0.0,0.0
    };

    public void init(NNffbpAlphaTabFast waterNet, NNffbpAlphaTabFast forwardWaterNet, AlgorithmParameter parameter) {
        this.waterNet = waterNet;
        this.forwardWaterNet = forwardWaterNet;
        this.spectrumOutOfScopeThreshold = parameter.spectrumOutOfScopeThreshold;
    }


    public double[] perform(double teta_sun_deg,
                            double teta_view_deg, double azi_diff_deg,
                            OutputBands outputBands) {

        /* determine cut_thresh from waterNet minimum */
        double cut_thresh = 1000.0;
        for (int i = 3; i < 11; i++) {
            double inmini = Math.exp(waterNet.inmin[i]);
            if (inmini < cut_thresh) {
                cut_thresh = inmini;
            }
        }

        // test RLw against lowest or cut value in NN and set in lower
        // start with  MERIS band 1 412
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
            waterInnet[i] = Math.log(RLw_cut[i-3]); /* bands 1-7 == 412 - 664 nm */
        }
        waterInnet[10] = Math.log(RLw_cut[8]); /* band 708 nm */


        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (!test_logRLw(waterInnet)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.WLR_OOR);
        }

        /* calculate concentrations using the water nn */
        double[] waterOutnet = waterNet.calc(waterInnet);

        double aGelbstoff = Math.exp(waterOutnet[2]);
        outputBands.setValue("a_gelbstoff", aGelbstoff);

        double aPig = Math.exp(waterOutnet[1]);
        outputBands.setValue("a_pig", aPig);

        double chlConc = 62.6 * Math.pow(aPig, 1.29);
        outputBands.setValue("chl_conc", chlConc);

        double bTsm = Math.exp(waterOutnet[0]);;
        outputBands.setValue("b_tsm", bTsm);
        double tsm = bTsm/0.95;
        outputBands.setValue("tsm", tsm);

        outputBands.setValue("a_total", aPig + aGelbstoff + tsm * 0.089);   // absorption of all water constituents

        /* test if concentrations are within training range */
        if (!test_watconc(bTsm, aPig, aGelbstoff)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.CONC_OOR);
        }

        /* do forward NN computation */
        double[] forwardWaterInnet = new double[6];
        forwardWaterInnet[0] = teta_sun_deg;
        forwardWaterInnet[1] = teta_view_deg;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log bTsm
        forwardWaterInnet[4] = waterOutnet[1]; // log aPig
        forwardWaterInnet[5] = waterOutnet[2]; // log aGelbstoff
        double[] forwardWaterOutnet = forwardWaterNet.calc(forwardWaterInnet);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = Math.pow(forwardWaterOutnet[0] - Math.log(RLw_cut[0]), 2) +
                           Math.pow(forwardWaterOutnet[1] - Math.log(RLw_cut[1]), 2) +
                           Math.pow(forwardWaterOutnet[2] - Math.log(RLw_cut[2]), 2) +
                           Math.pow(forwardWaterOutnet[3] - Math.log(RLw_cut[3]), 2) +
                           Math.pow(forwardWaterOutnet[4] - Math.log(RLw_cut[4]), 2) +
                           Math.pow(forwardWaterOutnet[5] - Math.log(RLw_cut[5]), 2) +
                           Math.pow(forwardWaterOutnet[6] - Math.log(RLw_cut[6]), 2) +
                           Math.pow(forwardWaterOutnet[8] - Math.log(RLw_cut[8]), 2);
        outputBands.setValue("chiSquare", chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.OOTR);
        }
        // compute k_min and z90_max RD 20060811
        double k_min = KMean.perform(bTsm, aPig, aGelbstoff);
        outputBands.setValue("K_min", k_min);
        outputBands.setValue("Z90_max", -1.0 / k_min);
        return RLw_cut;

    }

    /*-----------------------------------------------------------------------------------
     **	test water leaving radiances as input to neural network for out of training range
     **	if out of range set to lower or upper boundary value
    -----------------------------------------------------------------------------------*/
    private boolean test_logRLw(double[] innet) {
        for (int i = 0; i < innet.length; i++) {
            if (innet[i] > waterNet.inmax[i]) {
                innet[i] = waterNet.inmax[i];
               return false;
            }
            if (innet[i] < waterNet.inmin[i]) {
                innet[i] = waterNet.inmin[i];
                return false;
            }
        }
        return true;
    }

    /*-------------------------------------------------------------------------------
     **	test water constituents as output of neural network for out of training range
     **
    --------------------------------------------------------------------------------*/
    private boolean test_watconc(double bTsm, double aPig, double aGelbstoff) {
        double log_spm = Math.log(bTsm);
        double log_pig = Math.log(aPig);
        double log_gelb = Math.log(aGelbstoff);
        if (log_spm > waterNet.outmax[0] || log_spm < waterNet.outmin[0]) {
            return false;
        }
        if (log_pig > waterNet.outmax[1] || log_pig < waterNet.outmin[1]) {
            return false;
        }
        if (log_gelb > waterNet.outmax[2] || log_gelb < waterNet.outmin[2]) {
            return false;
        }
        return true;
    }

}
