package org.esa.beam.lakes.boreal.algorithm;

import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.algorithm.atmosphere.Tosa;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.3 $ $Date: 2007-07-13 06:42:01 $
 */
class Experimental {

    boolean active;

    public Experimental(boolean active) {
        this.active = active;
    }

    public void ensureValidBlueRlwReflectances(Tosa.Result tosa, OutputBands outputBands) {
        if(!active) {
            return;
        }
        double[] rpathr = outputBands.getDoubleValues("path_");
        double faktor = (tosa.rlTosa[0] - 0.001) / rpathr[0];
        if (faktor < 1.0) {
            for (int i = 0; i < rpathr.length; i++) {
                rpathr[i] *= faktor;
            }
        }
        outputBands.setValues("path_", rpathr);
    }

    public void doEstimatedPolCorr(Tosa.Result tosa, OutputBands outputBands) {
         if(!active) {
            return;
        }
        double diff;
        double[] rpathr = outputBands.getDoubleValues("path_");
        for (int i = 0; i < rpathr.length; i++) {
            rpathr[i] -= tosa.RL_path_rayl[i]; // removed unnecessary '* pol_corr_fak', cause it's zero (MP)
        }

        double pol_corr_fak = 0.03; /* 3% estimated overestimation due to polarisation */
        diff = (rpathr[0] - tosa.RL_path_rayl[0] * pol_corr_fak) - (tosa.rlTosa[0] - 0.001);
        if (diff > 0.0) {
            pol_corr_fak += diff / tosa.RL_path_rayl[0];
        }

        for (int i = 0; i < rpathr.length; i++) {
            rpathr[i] -= tosa.RL_path_rayl[i] * (pol_corr_fak);
        }

        outputBands.setValues("path_", rpathr);
    }

    public double test_sunglint(double teta_view_rad, double teta_sun_rad, double azi_diff_rad, double windspeed) {
        return 0;
    }
}
