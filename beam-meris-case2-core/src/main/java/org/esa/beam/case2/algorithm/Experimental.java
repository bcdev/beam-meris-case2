package org.esa.beam.case2.algorithm;

import org.esa.beam.case2.algorithm.atmosphere.Tosa;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.5 $ $Date: 2007-07-13 06:41:35 $
 */
public class Experimental {

    private boolean active;

    public Experimental(boolean active) {
        this.active = active;
    }

    public void ensureValidBlueRlwReflectances(Tosa.Result tosa, OutputBands outputBands) {
        if (!active) {
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
        if (!active) {
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
}