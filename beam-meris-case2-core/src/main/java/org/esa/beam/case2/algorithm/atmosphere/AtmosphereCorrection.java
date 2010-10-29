package org.esa.beam.case2.algorithm.atmosphere;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.Auxdata;
import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.algorithm.PixelData;
import org.esa.beam.case2.algorithm.polcorr.PolarizationCorrection;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;

import java.util.Arrays;

public class AtmosphereCorrection {

    private static final double MAX_TAU_FACTOR = 0.84;
    private static final double[] H2O_COR_POLY =new double []{
            0.3832989, 1.6527957, -1.5635101, 0.5311913}; // polynom coefficients for band708 correction

    private AlgorithmParameter parameter;
    private PolarizationCorrection polarizationCorrection;
    private NNffbpAlphaTabFast atmosphereNet;
    private Tosa tosa;
    private double[] atmoInnet;

    public void init(Auxdata auxdata, AlgorithmParameter parameter) {
        this.atmosphereNet = auxdata.getAtmosphericNet();
        this.parameter = parameter;
        tosa = new Tosa();
        tosa.init(parameter.performSmileCorrection, auxdata.getSmileAuxdata());
        atmoInnet = new double[16];
        polarizationCorrection = new PolarizationCorrection();
        polarizationCorrection.init(auxdata.getPolarizationNet());

    }

    public Tosa.Result perform(PixelData pixel, double azi_diff_rad,
                               double teta_view_rad, double teta_sun_rad,
                               OutputBands outputBands) {
        Tosa.Result tosaResult = tosa.perform(pixel, teta_view_rad, teta_sun_rad, azi_diff_rad);
        outputBands.setValues("tosa_reflec_", tosaResult.rlTosa);

        /* test if tosa reflectances are out of training range */
        if (!validTosaReflectance(tosaResult.rlTosa)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.TOSA_OOR);
        }
        if (pixel.solzen > atmosphereNet.getInmax()[0] || pixel.solzen < atmosphereNet.getInmin()[0]) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.SOLZEN);
        }

        /* test if solar zenith angle within training range */
        double x, y, z, azi_diff_deg;


        // water vapour correction for band 9 (708 nm)
        double rho_885 = pixel.toa_radiance[13] / pixel.solar_flux[13];
        double rho_900 = pixel.toa_radiance[14] / pixel.solar_flux[14];
        double x2 = rho_900 / rho_885;
        double trans708 = H2O_COR_POLY[0] + H2O_COR_POLY[1] * x2 + H2O_COR_POLY[2] * x2 * x2 + H2O_COR_POLY[3] * x2 * x2 * x2;
        tosaResult.rlTosa[8] /= trans708;

        //if(atm_net.inmax[2]> 10)
        /* calculate Ed_boa, transmittance, path radiance for first 9 channels using NN */
        // calculate xyz coordinates
        x = -Math.sin(teta_view_rad) * Math.cos(azi_diff_rad);
        y = Math.abs(-Math.sin(teta_view_rad) * Math.sin(azi_diff_rad));
        z = Math.cos(teta_view_rad);
        azi_diff_deg = Math.toDegrees(azi_diff_rad);


        atmoInnet[0] = Math.toDegrees(teta_sun_rad); //input is teta_sun_deg
        atmoInnet[1] = x;
        atmoInnet[2] = y;
        atmoInnet[3] = z;
        atmoInnet[4] = Math.log(tosaResult.rlTosa[0]);
        atmoInnet[5] = Math.log(tosaResult.rlTosa[1]);
        atmoInnet[6] = Math.log(tosaResult.rlTosa[2]);
        atmoInnet[7] = Math.log(tosaResult.rlTosa[3]);
        atmoInnet[8] = Math.log(tosaResult.rlTosa[4]);
        atmoInnet[9] = Math.log(tosaResult.rlTosa[5]);
        atmoInnet[10] = Math.log(tosaResult.rlTosa[6]);
        atmoInnet[11] = Math.log(tosaResult.rlTosa[7]); // in the simulation of the water forward NN this band is 670 nm
        atmoInnet[12] = Math.log(tosaResult.rlTosa[8]);
        atmoInnet[13] = Math.log(tosaResult.rlTosa[9]);
        atmoInnet[14] = Math.log(tosaResult.rlTosa[10]);
        atmoInnet[15] = Math.log(tosaResult.rlTosa[11]);


        double[] atmoOutnet = atmosphereNet.calc(atmoInnet);

        double cosTetaSunRad = Math.cos(teta_sun_rad);
        for (int i = 0; i < 12; i++) {
            atmoOutnet[i] = Math.exp(atmoOutnet[i]);
            atmoOutnet[i + 12] = Math.exp(atmoOutnet[i + 12]);
            atmoOutnet[i + 24] = Math.exp(atmoOutnet[i + 24]) / cosTetaSunRad; //outnet is Ed_boa, not transmittance
        }

        // deriving reflec from rwPath
//        double cosTetaViewRad = Math.cos(teta_view_rad);
//        final double[] transds = Arrays.copyOfRange(atmoOutnet, 24, 36);
//        final double[] rwPaths = Arrays.copyOfRange(atmoOutnet, 12, 24);
//        final double[] reflec = Arrays.copyOfRange(atmoOutnet, 0, 12);
//        if (parameter.deriveRwFromPath) {
//            for (int i = 0; i < reflec.length; i++) {
//                final double v = transds[i];
//                double transu = Math.exp(Math.log(v) * (cosTetaSunRad / cosTetaViewRad));
//                reflec[i] = (tosaResult.rlTosa[i] - rwPaths[i]) / transu;
//            }
//        }
//
        outputBands.setValues("reflec_", Arrays.copyOfRange(atmoOutnet, 0, 10));
        outputBands.setValues("path_", Arrays.copyOfRange(atmoOutnet, 12, 22));
        outputBands.setValues("trans_", Arrays.copyOfRange(atmoOutnet, 24, 34));
        // skip 11
        outputBands.setValue("reflec_12", atmoOutnet[10]);
        outputBands.setValue("reflec_13", atmoOutnet[11]);
        outputBands.setValue("path_12", atmoOutnet[22]);
        outputBands.setValue("path_13", atmoOutnet[23]);
        outputBands.setValue("trans_12", atmoOutnet[34]);
        outputBands.setValue("trans_13", atmoOutnet[35]);

        if (parameter.performPolCorr) {
            double[] transmittance = outputBands.getDoubleValues("trans_");
            double[] polCorrected = polarizationCorrection.perform(pixel, azi_diff_deg, transmittance,
                                                                   tosaResult.rlTosa);
            outputBands.setValues("tosa_", polCorrected);
            System.arraycopy(polCorrected, 0, tosaResult.rlTosa, 0, 9);
        }

        /* compute angstrom coefficient from band 12 and 13 778 and 865 nm */
        double ang_443_865 = -Math.log(atmoOutnet[36] / atmoOutnet[39]) / Math.log(
                AtmosphereConstants.merband12[1] / AtmosphereConstants.merband12[11]);
        outputBands.setValue("ang_443_865", ang_443_865);
        outputBands.setValue("tau_550", atmoOutnet[37]);

        if (outputBands.getDoubleValue("tau_550") > atmosphereNet.getOutmax()[37] * MAX_TAU_FACTOR) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.ATC_OOR);
        }

        if(atmoOutnet[40] > atmosphereNet.getOutmax()[40] * 0.97) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.SUNGLINT);
        }


        return tosaResult;
    }

    /*--------------------------------------------------------------------------
     **	test TOSA radiances as input to neural network for out of training range
     **  with band_nu 17/3/05 R.D.
    --------------------------------------------------------------------------*/
    private boolean validTosaReflectance(double[] rL_tosa) {
        for (int i = 0; i < rL_tosa.length; i++) {
            double currentRlTosa = Math.log(rL_tosa[i]);
            final int netIndex = i + 4;
            if (currentRlTosa > atmosphereNet.getInmax()[netIndex] || currentRlTosa < atmosphereNet.getInmin()[netIndex]) {
                return false;
            }
        }
        return true;
    }

}
