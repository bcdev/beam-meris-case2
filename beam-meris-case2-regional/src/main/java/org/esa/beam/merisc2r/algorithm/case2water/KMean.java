package org.esa.beam.merisc2r.algorithm.case2water;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.2 $ $Date: 2007-07-12 15:39:21 $
 */
class KMean {

    private static final double[] a_wat_mer8 = {
            0.004614, 0.006814, 0.0150, 0.0325,
            0.061440, 0.273960, 0.4252, 0.7778};
    private static final double[] b_wat_mer8 = {
            0.0066464, 0.0049059, 0.0031426, 0.0026439,
            0.0017788, 0.0011451, 0.0008456, 0.0006409};
    private static final double[] a_pig_mer8 = {
            0.9150741, 1.0000000, 0.6055960, 0.4786963,
            0.2447868, 0.2341397, 0.4440685, 0.0491615};
    private static final double[] a_gelb_mer8 = {
            1.4799377, 0.9723884, 0.4965853, 0.3753111,
            0.1890016, 0.0815940, 0.0434563, 0.0234708};
    private static final double[] b_tsm_mer8 = {
            1.0266495, 0.9981876, 0.9578613, 0.9426555,
            0.9086913, 0.8723782, 0.8482304, 0.8267377};

    public static double perform(double bTsm, double aPig, double aGelbstoff) {
        double k_min = 1000.0;
        double[] k_mina3 = new double[3];
        k_mina3[0] = 1000.0;
        double k_mean = 0.0;
        double[] k_tot_mer8 = new double[8];
        for (int i = 0; i < k_tot_mer8.length; i++) {
            double a_tot_mer8 = a_wat_mer8[i] + aPig * a_pig_mer8[i] + aGelbstoff * a_gelb_mer8[i];
            double bb_tot_mer8 = 0.5 * b_wat_mer8[i] + 0.05 * bTsm * b_tsm_mer8[i];
            k_tot_mer8[i] = Math.sqrt(a_tot_mer8 * (a_tot_mer8 + 2.0 * bb_tot_mer8)); //from Joseph 2flow equation
            k_mean += k_tot_mer8[i];
            if (k_tot_mer8[i] < k_min) {
                k_min = k_tot_mer8[i];
            }
            if (k_tot_mer8[i] < k_mina3[0]) {
                k_mina3[2] = k_mina3[1];
                k_mina3[1] = k_mina3[0];
                k_mina3[0] = k_tot_mer8[i];
            } else if (k_tot_mer8[i] < k_mina3[1]) {
                k_mina3[2] = k_mina3[1];
                k_mina3[1] = k_tot_mer8[i];
            } else if (k_tot_mer8[i] < k_mina3[2]) {
                k_mina3[2] = k_tot_mer8[2];
            }

        }

        k_mean = (k_mina3[0] + k_mina3[1] + k_mina3[2]) / 3.0;
        return k_mean;
    }
}
