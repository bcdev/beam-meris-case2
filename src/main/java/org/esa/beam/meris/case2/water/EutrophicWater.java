package org.esa.beam.meris.case2.water;

import org.esa.beam.framework.gpf.experimental.PointOperator;
import org.esa.beam.meris.case2.algorithm.KMin;


public class EutrophicWater extends WaterAlgorithm {

    public static final int TARGET_A_BTSM_INDEX = 10;

    private final double tsmExponent;
    private final double tsmFactor;
    private final double chlExponent;
    private final double chlFactor;

    public EutrophicWater(double spectrumOutOfScopeThreshold, double tsmExponent,
                          double tsmFactor, double chlExponent, double chlFactor) {
        super(spectrumOutOfScopeThreshold);
        this.tsmExponent = tsmExponent;
        this.tsmFactor = tsmFactor;
        this.chlExponent = chlExponent;
        this.chlFactor = chlFactor;
    }

    @Override
    protected KMin createKMin(PointOperator.WritableSample[] targetSamples) {
        final double bTsm = targetSamples[TARGET_B_TSM_INDEX].getDouble();
        final double aPig = targetSamples[TARGET_A_PIGMENT_INDEX].getDouble();
        final double aGelbstoff = targetSamples[TARGET_A_GELBSTOFF_INDEX].getDouble();
        final double aBtsm = targetSamples[TARGET_A_BTSM_INDEX].getDouble();

        final KMin kMin = new KMin(bTsm, aPig, aGelbstoff, aBtsm);
        kMin.setA_gelb_mer8(new double[]{
                1.9220648, 0.9934217, 0.3501478, 0.2260046,
                0.0832423, 0.0753961, 0.0201853, 0.0075169
        });
        return kMin;
    }

    @Override
    protected double computeChiSquare(double[] forwardWaterOutnet, double[] RLw_cut) {
        return Math.pow(forwardWaterOutnet[0] - Math.log(RLw_cut[1]), 2) + // it starts with 442 nm
               Math.pow(forwardWaterOutnet[1] - Math.log(RLw_cut[2]), 2) +
               Math.pow(forwardWaterOutnet[2] - Math.log(RLw_cut[3]), 2) +
               Math.pow(forwardWaterOutnet[3] - Math.log(RLw_cut[4]), 2) +
               Math.pow(forwardWaterOutnet[4] - Math.log(RLw_cut[5]), 2) +
               Math.pow(forwardWaterOutnet[5] - Math.log(RLw_cut[6]), 2) +
               Math.pow(forwardWaterOutnet[6] - Math.log(RLw_cut[8]), 2);
    }

    @Override
    protected double[] getForwardWaterInnet(double solzen, double satzen, double azi_diff_deg,
                                            double[] waterOutnet) {
        double[] forwardWaterInnet = new double[7];
        forwardWaterInnet[0] = solzen;
        forwardWaterInnet[1] = satzen;
        forwardWaterInnet[2] = azi_diff_deg;
        forwardWaterInnet[3] = waterOutnet[0]; // log gelbstoff
        forwardWaterInnet[4] = waterOutnet[1]; // log a_btsm
        forwardWaterInnet[5] = waterOutnet[2]; // log pigment
        forwardWaterInnet[6] = waterOutnet[3]; // log tsm
        return forwardWaterInnet;


    }

    @Override
    protected void fillOutput(double[] waterOutnet, PointOperator.WritableSample[] targetSamples) {
        double bTsm = Math.exp(waterOutnet[3]);
        targetSamples[TARGET_B_TSM_INDEX].set(bTsm);
        targetSamples[TARGET_TSM_INDEX].set(Math.exp(Math.log(tsmFactor) + waterOutnet[3] * tsmExponent));

        double aPig = Math.exp(waterOutnet[2]) * chlFactor;
        targetSamples[TARGET_A_PIGMENT_INDEX].set(aPig);
//        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(Math.log(chlFactor) + waterOutnet[2] * chlExponent));
        targetSamples[TARGET_CHL_CONC_INDEX].set(Math.exp(Math.log(1.0) + waterOutnet[2] * chlExponent));

        double aGelbstoff = Math.exp(waterOutnet[0]);
        targetSamples[TARGET_A_GELBSTOFF_INDEX].set(aGelbstoff);
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff);

        double aBtsm = Math.exp(waterOutnet[1]);
        targetSamples[TARGET_A_BTSM_INDEX].set(bTsm); // bleached suspended matter absorption at 442
        targetSamples[TARGET_A_TOTAL_INDEX].set(aPig + aGelbstoff + aBtsm);
    }

    @Override
    protected double[] getWaterInnet(double solzen, double satzen, double azi_diff_deg, double[] RLw_cut) {
        double[] waterInnet = new double[10];

        for (int i = 0; i < 6; i++) {
            waterInnet[i] = Math.log(RLw_cut[i + 1]); /* bands 2-7 == 442 - 664 nm */
        }
        waterInnet[6] = Math.log(RLw_cut[8]); /* band 708 nm */
        waterInnet[7] = solzen;
        waterInnet[8] = satzen;
        waterInnet[9] = azi_diff_deg;

        return waterInnet;

    }

    @Override
    protected double getCutThreshold(double[] inmin) {
        double cut_thresh = 1000.0;
        for (int i = 0; i < 7; i++) {
            double inmini = Math.exp(inmin[i]);
            if (inmini < cut_thresh) {
                cut_thresh = inmini;
            }
        }
        return cut_thresh;

    }
}
