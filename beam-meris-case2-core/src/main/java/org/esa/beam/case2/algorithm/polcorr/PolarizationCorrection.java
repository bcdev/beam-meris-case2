package org.esa.beam.case2.algorithm.polcorr;

import org.esa.beam.case2.algorithm.PixelData;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;


public class PolarizationCorrection {

    private NNffbpAlphaTabFast polarizationNet;

    public void init(NNffbpAlphaTabFast polarizationNet) {
        this.polarizationNet = polarizationNet;
    }

    public double[] perform(PixelData pixel, double azi_diff_deg, double[] ed_boa, double[] rltosa) {
        double[] inPolKorr = new double[21];
        inPolKorr[0] = pixel.solzen;
        inPolKorr[1] = pixel.satzen;
        inPolKorr[2] = azi_diff_deg;
        double cos_teta_sun = Math.cos(pixel.solzen * 3.1416 / 180.0);
        for (int i = 0; i < 9; i++) {
            inPolKorr[i + 3] = rltosa[i];
            inPolKorr[i + 12] = ed_boa[i] / cos_teta_sun;
        }
        double[] outPolKorr = polarizationNet.calc(inPolKorr);

        double[] corrRltosa = new double[9];
        for (int i = 0; i < 9; i++) {
            corrRltosa[i] = rltosa[i] / outPolKorr[i];
        }
        return corrRltosa;
    }

}
