package org.esa.beam.lakes.eutrophic.algorithm.case2water;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.5 $ $Date: 2007-07-12 12:10:43 $
 */
public class EutrophicWater {
    private NNffbpAlphaTabFast waterNet;
    private NNffbpAlphaTabFast forwardWaterNet;

    private double tsmExponent;
    private double tsmFactor;
    private double chlExponent;
    private double chlFactor;

    private double spectrumOutOfScopeThreshold;

    public void init(NNffbpAlphaTabFast waterNet, NNffbpAlphaTabFast forwardWaterNet,AlgorithmParameter parameter) {
        this.waterNet = waterNet;
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
        for (int i = 0; i < 7; i++) {
            double inmini = Math.exp(waterNet.inmin[i]);
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
        double[] waterInnet = new double[10];
        waterInnet[7] = teta_sun_deg;
        waterInnet[8] = teta_view_deg;
        waterInnet[9] = azi_diff_deg;
        for (int i = 0; i < 6; i++) {
            waterInnet[i ] = Math.log(RLw_cut[i+1]); /* bands 1-7 == 442 - 664 nm */
        }
        waterInnet[6] = Math.log(RLw_cut[8]); /* band 708 nm */

        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (!test_logRLw(waterInnet)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.WLR_OOR);
        }

        /* calculate concentrations using the water nn */
        double[] waterOutnet = waterNet.calc(waterInnet);

        double bTsm = Math.exp(waterOutnet[3]);
        outputBands.setValue("b_tsm", bTsm);
        outputBands.setValue("tsm", Math.exp(Math.log(tsmFactor) + waterOutnet[3] * tsmExponent));

        double aPig = Math.exp(waterOutnet[2])*chlFactor;
        outputBands.setValue("a_pig", aPig);
        //outputBands.setValue("chl_conc", Math.exp(Math.log(chlFactor) + waterOutnet[2] * chlExponent));
        outputBands.setValue("chl_conc", Math.exp(Math.log(1.0) + waterOutnet[2] * chlExponent));
        double aGelbstoff = Math.exp(waterOutnet[0]);
        outputBands.setValue("a_gelbstoff", aGelbstoff);
        double aBtsm = Math.exp(waterOutnet[1]);
        outputBands.setValue("a_btsm", aBtsm); // bleached suspended matter absorption at 442
        outputBands.setValue("a_total", aPig + aGelbstoff+aBtsm);

        /* test if concentrations are within training range */
        if (!test_watconc(bTsm, aPig, aGelbstoff)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.CONC_OOR);
        }

        /* do forward NN computation */
        double[] forwardWaterInnet = new double[7];
        forwardWaterInnet[0] = teta_sun_deg;
        forwardWaterInnet[1] = teta_view_deg;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log gelbstoff
        forwardWaterInnet[4] = waterOutnet[1]; // log a_btsm
        forwardWaterInnet[5] = waterOutnet[2]; // log pigment
        forwardWaterInnet[6] = waterOutnet[3]; // log tsm
        double[] forwardWaterOutnet = forwardWaterNet.calc(forwardWaterInnet);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = Math.pow(forwardWaterOutnet[0] - Math.log(RLw_cut[1]), 2) + /* it starts with 442 nm */
                           Math.pow(forwardWaterOutnet[1] - Math.log(RLw_cut[2]), 2) +
                           Math.pow(forwardWaterOutnet[2] - Math.log(RLw_cut[3]), 2) +
                           Math.pow(forwardWaterOutnet[3] - Math.log(RLw_cut[4]), 2) +
                           Math.pow(forwardWaterOutnet[4] - Math.log(RLw_cut[5]), 2) +
                           Math.pow(forwardWaterOutnet[5] - Math.log(RLw_cut[6]), 2) +
                           Math.pow(forwardWaterOutnet[6] - Math.log(RLw_cut[8]), 2);/* in outnet 7 corresponds to RLw 8 */
                        

        outputBands.setValue("chiSquare", chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.OOTR);

        }
        // compute k_min and z90_max RD 20060811
        double k_min = EutrophicKMean.perform(bTsm, aPig, aGelbstoff, aBtsm);
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
