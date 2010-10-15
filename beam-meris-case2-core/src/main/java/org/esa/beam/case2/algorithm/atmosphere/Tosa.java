package org.esa.beam.case2.algorithm.atmosphere;

import org.esa.beam.case2.algorithm.PixelData;
import org.esa.beam.meris.radiometry.smilecorr.SmileCorrectionAuxdata;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.3 $ $Date: 2007-07-13 06:42:01 $
 */
public class Tosa {


    public static class Result {

        public Result(double[] RL_path_rayl, double[] rlTosa, double[] tau_rayl_standard) {
            this.RL_path_rayl = RL_path_rayl;
            this.rlTosa = rlTosa;
            this.tau_rayl_standard = tau_rayl_standard;
        }

        public double[] rlTosa;
        public double[] tau_rayl_standard;
        public double[] RL_path_rayl;
}

    private boolean doSmileCorrection;
    private SmileCorrectionAuxdata smileAuxData;

    private double[] trans_oz_toa_tosa_down;
    private double[] trans_oz_toa_tosa_up;
    private double[] tau_rayl_toa_tosa;
    private double[] trans_oz_tosa_down;
    private double[] trans_oz_tosa_up;
    private double[] tau_rayl_nosmile_tosa;
    private double[] tau_rayl_smile_tosa;
    private double[] trans_rayl_down;
    private double[] trans_rayl_up;
    private double[] lrcPath;
    private double[] ed_toa;
    private double[] edTosa;
    private double[] lTosa;
    private double[] lrcTosaSmileCor;

    public void init(boolean performSmileCorrection, SmileCorrectionAuxdata smileAuxData) {
        this.doSmileCorrection = performSmileCorrection;
        this.smileAuxData = smileAuxData;
        int length = 12;
        trans_oz_toa_tosa_down = new double[length];
        trans_oz_toa_tosa_up = new double[length];
        tau_rayl_toa_tosa = new double[length];
        trans_oz_tosa_down = new double[length];
        trans_oz_tosa_up = new double[length];
        tau_rayl_nosmile_tosa = new double[length];
        tau_rayl_smile_tosa = new double[length];
        trans_rayl_down = new double[length];
        trans_rayl_up = new double[length];
        lrcPath = new double[length];
        ed_toa = new double[length];
        edTosa = new double[length];
        lTosa = new double[length];
        lrcTosaSmileCor = new double[length];
    }

    public Result perform(PixelData pixel, double teta_view_rad, double teta_sun_rad, double azi_diff_rad) {

        /* angles */
        double cos_teta_sun = Math.cos(teta_sun_rad);
        double sin_teta_sun = Math.sin(teta_sun_rad);
        double cos_teta_view = Math.cos(teta_view_rad);
        double sin_teta_view = Math.sin(teta_view_rad);
        double cos_azi_diff = Math.cos(azi_diff_rad);

        double altitude_pressure;
        double[] RL_path_rayl = new double[9];
        double[] rlTosa = new double[12];
        double[] tau_rayl_standard = new double[12];

        double[] sun_toa;
        if(doSmileCorrection){
            sun_toa = retrieveToaFrom(doSmileCorrection(pixel.detectorIndex, pixel.solar_flux, smileAuxData));
        }else {
            sun_toa = retrieveToaFrom(pixel.solar_flux);
        }
        double[] lToa = retrieveToaFrom(pixel.toa_radiance);

        /* compute Ed_toa from sun_toa using  cos_teta_sun */
        for (int i = 0; i < ed_toa.length; i++) {
            ed_toa[i] = sun_toa[i] * cos_teta_sun;
        }

        /* calculate relative airmass rayleigh correction for correction layer*/
        if(pixel.altitude < 1.0){
            pixel.altitude= (float)1.0;
        }
        altitude_pressure=pixel.pressure*Math.pow((1.0-0.0065*pixel.altitude/288.15),5.255);

        double rayl_rel_mass_toa_tosa = (altitude_pressure - 1013.2) / 1013.2;

        double[] detectorWavelength = null;

        /* calculate optical thickness of rayleigh for correction layer, lam in micrometer */
        for (int i = 0; i < tau_rayl_standard.length; i++) {
            tau_rayl_standard[i] = 0.008735 * Math.pow(AtmosphereConstants.merband12[i] / 1000.0, -4.08);/* lam in µm */
            tau_rayl_toa_tosa[i] = tau_rayl_standard[i] * rayl_rel_mass_toa_tosa;
            if (doSmileCorrection) {
                if(detectorWavelength == null) {
                    detectorWavelength = smileAuxData.getDetectorWavelengths()[pixel.detectorIndex];
                }
                int ilami = AtmosphereConstants.merband12_index[i];
                double tau_rayl_smile_rat = Math.pow(detectorWavelength[ilami] / 1000.0, -4.08) /
                                            Math.pow(smileAuxData.getTheoreticalWavelengths()[ilami] / 1000.0, -4.08);
                tau_rayl_nosmile_tosa[i] = tau_rayl_standard[i];
                tau_rayl_smile_tosa[i] = tau_rayl_standard[i] * tau_rayl_smile_rat;
            }
        }

        /* calculate phase function for rayleigh path radiance*/
        double cos_scat_ang = -cos_teta_view * cos_teta_sun - sin_teta_view * sin_teta_sun * cos_azi_diff;
        double phase_rayl = 0.75 * (1.0 + cos_scat_ang * cos_scat_ang);
        double cos_gamma_plus = cos_teta_view * cos_teta_sun - sin_teta_view * sin_teta_sun * cos_azi_diff;
        double phase_rayleigh_plus = 0.75 * (1.0 + cos_gamma_plus * cos_gamma_plus);
        double fresnel_reflect = sub_fresnel(teta_view_rad) + sub_fresnel(teta_sun_rad);
        double rayleigh_reflect = fresnel_reflect * phase_rayleigh_plus;

        /* ozon and rayleigh correction layer transmission */
        double ozon_rest_mass = (pixel.ozone / 1000.0 - 0.35); /* conc ozone from MERIS is in DU */
        for (int i = 0; i < trans_oz_toa_tosa_down.length; i++) {
            trans_oz_toa_tosa_down[i] = Math.exp(-AtmosphereConstants.absorb_ozon[i] * ozon_rest_mass / cos_teta_sun);
            trans_oz_toa_tosa_up[i] = Math.exp(-AtmosphereConstants.absorb_ozon[i] * ozon_rest_mass / cos_teta_view);
            trans_rayl_down[i] = Math.exp(-tau_rayl_toa_tosa[i] * 0.5 / cos_teta_sun); /* 0.5 because diffuse trans */
            trans_rayl_up[i] = Math.exp(-tau_rayl_toa_tosa[i] * 0.5 / cos_teta_view);
            trans_oz_tosa_down[i] = Math.exp(-AtmosphereConstants.absorb_ozon[i] * pixel.ozone / 1000.0 / cos_teta_sun);
            trans_oz_tosa_up[i] = Math.exp(-AtmosphereConstants.absorb_ozon[i] * pixel.ozone / 1000.0 / cos_teta_view);
        }

//        if (doSmileCorrection) { // the 2 lines now in the loop above RD20070830
        if (false) {
            for (int i = 0; i < trans_oz_tosa_down.length; i++) {
                trans_oz_tosa_down[i] = Math.exp(-AtmosphereConstants.absorb_ozon[i] * pixel.ozone / 1000.0 / cos_teta_sun);
                trans_oz_tosa_up[i] = Math.exp(-AtmosphereConstants.absorb_ozon[i] * pixel.ozone / 1000.0 / cos_teta_view);
            }
        }
        /* Rayleigh path radiance of correction layer */

        for (int i = 0; i < lrcPath.length; i++) {
            lrcPath[i] = ed_toa[i] * tau_rayl_toa_tosa[i] * trans_oz_tosa_down[i]
                             * phase_rayl / (4 * Math.PI * cos_teta_view * cos_teta_sun);
        }

        /* Calculate Ed_tosa */

        for (int i = 0; i < edTosa.length; i++) {
            edTosa[i] = ed_toa[i] * trans_oz_toa_tosa_down[i] * trans_rayl_down[i];
        }

        /* compute path radiance difference for tosa without - with smile */
        for (int i = 0; i < lTosa.length; i++) {
            /* Calculate L_tosa */
            lTosa[i] = (lToa[i] - lrcPath[i]*trans_oz_tosa_up[i])/ trans_oz_toa_tosa_up[i];

            if (doSmileCorrection) {
                double zwi = ed_toa[i] * trans_oz_tosa_down[i] * trans_oz_tosa_up[i] * (phase_rayl + rayleigh_reflect) / (4 * Math.PI * cos_teta_view * cos_teta_sun);
                lrcTosaSmileCor[i] = (tau_rayl_nosmile_tosa[i] - tau_rayl_smile_tosa[i]) * zwi;
                lTosa[i] -= lrcTosaSmileCor[i];
            }
            /* Calculate Lsat_tosa radiance reflectance as input to NN */
            rlTosa[i] = lTosa[i] / edTosa[i];
        }

        /* calculate Rayleigh path radiance reflectance for polarisation correction factor*/
        /* Rayleigh path radiance of correction layer */
        for (int i = 0; i < RL_path_rayl.length; i++) {
            //RL_path_rayl[ilam] = tau_rayl[ilam] * phase_rayl / (4 * Math.PI * cos_teta_view);
            RL_path_rayl[i] = tau_rayl_standard[i] * trans_oz_tosa_down[i] * trans_oz_tosa_up[i]
                                      * (phase_rayl + rayleigh_reflect) / (4 * Math.PI * cos_teta_view);
        }
        return new Result(RL_path_rayl, rlTosa, tau_rayl_standard);
    }

    private static double[] doSmileCorrection(int detectorIndex, double[] solarFlux, SmileCorrectionAuxdata smileAuxData){
        /* correct solar flux for this pixel */
        double[] solarFluxSmile = new double[solarFlux.length];
        double[] detectorSunSpectralFlux = smileAuxData.getDetectorSunSpectralFluxes()[detectorIndex];
        double[] theoreticalSunSpectralFluxes = smileAuxData.getTheoreticalSunSpectralFluxes();
        for (int i = 0; i < solarFlux.length; i++) {
            solarFluxSmile[i] = solarFlux[i] * (detectorSunSpectralFlux[i] / theoreticalSunSpectralFluxes[i]);
        }
        return solarFluxSmile;
    }

    private static double[] retrieveToaFrom(double[] values) {
        double[] toa = new double[12];
        System.arraycopy(values, 0, toa, 0, 10);
        System.arraycopy(values, 11, toa, 10, 2);
        return toa;
    }

    /* ---------------------------------------------------------------------------
         ** fresnel reflection calculation
         ** doerffer, 18.9.2005
        ----------------------------------------------------------------------------*/
    private static double sub_fresnel(double teta) {
        double rho;
        if (teta < 0.02) /* for near nadir direction */ {
            rho = 0.02;
        } else {
            double teta_sub = Math.asin(Math.sin(teta) / 1.34);// 1.34 is refraction index water
            double add_teta = teta + teta_sub;
            double sub_teta = teta - teta_sub;
            double sin_sub_teta = Math.sin(sub_teta);
            double sin_add_teta = Math.sin(add_teta);
            double tan_sub_teta = Math.tan(sub_teta);
            double tan_add_teta = Math.tan(add_teta);

            rho = 0.5 * ((sin_sub_teta * sin_sub_teta) / (sin_add_teta * sin_add_teta)
                         + (tan_sub_teta * tan_sub_teta) / (tan_add_teta * tan_add_teta));
        }
        return rho;
    }
}
