package org.esa.beam.meris.case2;

import org.esa.beam.case2.algorithm.KMin;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;
import org.esa.beam.framework.gpf.experimental.PointOperator;

import static org.esa.beam.meris.case2.MerisCase2WaterOp.*;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.5 $ $Date: 2007-07-12 12:10:43 $
 */
public class Case2Water {

    private final double tsmExponent;
    private final double tsmFactor;
    private final double chlExponent;
    private final double chlFactor;

    private double spectrumOutOfScopeThreshold;

    public Case2Water(double tsmExponent, double tsmFactor, double chlExponent, double chlFactor,
                      double spectrumOutOfScopeThreshold) {
        this.tsmExponent = tsmExponent;
        this.tsmFactor = tsmFactor;
        this.chlExponent = chlExponent;
        this.chlFactor = chlFactor;
        this.spectrumOutOfScopeThreshold = spectrumOutOfScopeThreshold;
    }

    public double[] perform(NNffbpAlphaTabFast waterNet, NNffbpAlphaTabFast forwardWaterNet,
                            double solzen, double satzen, double azi_diff_deg, PointOperator.Sample[] sourceSamples,
                            PointOperator.WritableSample[] targetSamples) {

        /* determine cut_thresh from waterNet minimum */
        double cut_thresh = 1000.0;
        final double[] inmin = waterNet.getInmin();
        for (int i = 0; i < 8; i++) {
            double inmini = Math.exp(inmin[i + 3]);
            if (inmini < cut_thresh) {
                cut_thresh = inmini;
            }
        }

        // test RLw against lowest or cut value in NN and set in lower
        // start with  MERIS band 2 443

        double[] RLw = new double[8];
        RLw[0] = sourceSamples[0].getDouble();
        RLw[1] = sourceSamples[1].getDouble();
        RLw[2] = sourceSamples[2].getDouble();
        RLw[3] = sourceSamples[3].getDouble();
        RLw[4] = sourceSamples[4].getDouble();
        RLw[5] = sourceSamples[5].getDouble();
        RLw[6] = sourceSamples[6].getDouble();
        RLw[7] = sourceSamples[7].getDouble();
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
        double[] waterInnet = new double[11];
        waterInnet[0] = solzen;
        waterInnet[1] = satzen;
        waterInnet[2] = azi_diff_deg;
        for (int i = 0; i <= 7; i++) {
            waterInnet[i + 3] = Math.log(RLw_cut[i]); /* bands 1-7, 9 == 412 - 664 nm,  708 nm*/
        }

        // test if water leaving radiance reflectance are within training range,
        // otherwise set to training range
        if (!test_logRLw(waterInnet, waterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(WLR_OOR_BIT_INDEX, true);
        }

        /* calculate concentrations using the water nn */
        double[] waterOutnet = waterNet.calc(waterInnet);

        double bTsm = Math.exp(waterOutnet[0]);
        targetSamples[TARGET_B_TSM_INDEX].set(bTsm);
        targetSamples[TARGET_TSM_INDEX].set(Math.exp(Math.log(tsmFactor) + waterOutnet[0] * tsmExponent));

        double aPig = Math.exp(waterOutnet[1]);
        targetSamples[TARGET_A_PIGMENT_INDEX].set(aPig);
        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(Math.log(chlFactor) + waterOutnet[1] * chlExponent));
        double aGelbstoff = Math.exp(waterOutnet[2]);
        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(aGelbstoff);
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff);

        /* test if concentrations are within training range */
        if (!test_watconc(bTsm, aPig, aGelbstoff, waterNet)) {
            targetSamples[TARGET_FLAG_INDEX].set(CONC_OOR_BIT_INDEX, true);
        }

        /* do forward NN computation */
        double[] forwardWaterInnet = new double[6];
        forwardWaterInnet[0] = solzen;
        forwardWaterInnet[1] = satzen;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log gelbstoff
        forwardWaterInnet[4] = waterOutnet[1]; // log pigment
        forwardWaterInnet[5] = waterOutnet[2]; // log tsm
        double[] forwardWaterOutnet = forwardWaterNet.calc(forwardWaterInnet);

        /* compute chi square deviation on log scale between measured and computed spectrum */
        double chiSquare = 0;
        for (int i = 0; i <= 7; i++) {
            chiSquare += Math.pow(forwardWaterOutnet[i] - Math.log(RLw_cut[i]), 2);
        }

        targetSamples[TARGET_CHI_SQUARE_INDEX].set(chiSquare);

        if (chiSquare > spectrumOutOfScopeThreshold) {
            targetSamples[TARGET_FLAG_INDEX].set(OOTR_BIT_INDEX, true);
        }
        // compute k_min and z90_max RD 20060811
        final KMin kMin = new KMin();
        double k_min = kMin.perform(bTsm, aPig, aGelbstoff, 0);
        targetSamples[TARGET_K_MIN_INDEX].set(k_min);
        targetSamples[TARGET_Z90_MAX_INDEX].set(-1.0 / k_min);
        return RLw_cut;
    }

    /*-----------------------------------------------------------------------------------
     **	test water leaving radiances as input to neural network for out of training range
     **	if out of range set to lower or upper boundary value
    -----------------------------------------------------------------------------------*/

    private boolean test_logRLw(double[] innet, NNffbpAlphaTabFast waterNet) {
        final double[] inmax = waterNet.getInmax();
        for (int i = 0; i < innet.length; i++) {
            if (innet[i] > inmax[i]) {
                innet[i] = inmax[i];
                return false;
            }
            final double[] inmin = waterNet.getInmin();
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

    private boolean test_watconc(double bTsm, double aPig, double aGelbstoff, NNffbpAlphaTabFast waterNet) {
        double log_spm = Math.log(bTsm);
        double log_pig = Math.log(aPig);
        double log_gelb = Math.log(aGelbstoff);
        final double[] outmax = waterNet.getOutmax();
        final double[] outmin = waterNet.getOutmin();
        final boolean ootr0 = log_spm > outmax[0] || log_spm < outmin[0];
        final boolean ootr1 = log_pig > outmax[1] || log_pig < outmin[1];
        final boolean ootr2 = log_gelb > outmax[2] || log_gelb < outmin[2];
        return !(ootr0 || ootr1 || ootr2);
    }

}
