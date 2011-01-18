package org.esa.beam.meris.case2.algorithm;

public class KMin {

    private double[] a_wat_mer8 = {
            0.004614, 0.006814, 0.0150, 0.0325,
            0.061440, 0.273960, 0.4252, 0.7778
    };
    private double[] b_wat_mer8 = {
            0.0066464, 0.0049059, 0.0031426, 0.0026439,
            0.0017788, 0.0011451, 0.0008456, 0.0006409
    };
    private double[] a_pig_mer8 = {
            0.9150741, 1.0000000, 0.6055960, 0.4786963,
            0.2447868, 0.2341397, 0.4440685, 0.0491615
    };
    private double[] a_gelb_mer8 = {
            1.4799377, 0.9723884, 0.4965853, 0.3753111,
            0.1890016, 0.0815940, 0.0434563, 0.0234708
    };
    private double[] b_tsm_mer8 = {
            1.0266495, 0.9981876, 0.9578613, 0.9426555,
            0.9086913, 0.8723782, 0.8482304, 0.8267377
    };
    private double[] a_btsm_mer8 = {
            1.2682018, 0.9976029, 0.6827681, 0.5822822,
            0.4049466, 0.3906278, 0.2419075, 0.1689082
    };
    private double bTsm;
    private double aPig;
    private double aGelbstoff;
    private double aBtsm;

    public KMin(double bTsm, double aPig, double aGelbstoff) {
        this(bTsm, aPig, aGelbstoff, 0);
    }

    public KMin(double bTsm, double aPig, double aGelbstoff, double aBtsm) {
        this.bTsm = bTsm;
        this.aPig = aPig;
        this.aGelbstoff = aGelbstoff;
        this.aBtsm = aBtsm;
    }

    public double[] getA_wat_mer8() {
        return a_wat_mer8;
    }

    public void setA_wat_mer8(double[] a_wat_mer8) {
        this.a_wat_mer8 = a_wat_mer8.clone();
    }

    public double[] getB_wat_mer8() {
        return b_wat_mer8;
    }

    public void setB_wat_mer8(double[] b_wat_mer8) {
        this.b_wat_mer8 = b_wat_mer8.clone();
    }

    public double[] getA_pig_mer8() {
        return a_pig_mer8;
    }

    public void setA_pig_mer8(double[] a_pig_mer8) {
        this.a_pig_mer8 = a_pig_mer8.clone();
    }

    public double[] getA_gelb_mer8() {
        return a_gelb_mer8;
    }

    public void setA_gelb_mer8(double[] a_gelb_mer8) {
        this.a_gelb_mer8 = a_gelb_mer8.clone();
    }

    public double[] getA_btsm_mer8() {
        return a_btsm_mer8;
    }

    public void setB_tsm_mer8(double[] b_tsm_mer8) {
        this.b_tsm_mer8 = b_tsm_mer8.clone();
    }

    public double[] getB_tsm_mer8() {
        return b_tsm_mer8;
    }

    public void setA_btsm_mer8(double[] a_btsm_mer8) {
        this.a_btsm_mer8 = a_btsm_mer8.clone();
    }

    public double computeKMinValue() {
        double k_min = 1000.0;
        double[] k_mina3 = new double[3];
        k_mina3[0] = 1000.0;
        double k_mean = 0.0;
        double[] k_tot_mer8 = new double[8];
        for (int i = 0; i < k_tot_mer8.length; i++) {
            k_tot_mer8[i] = computeKdAtIndex(i);
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


    public double computeKd490() {
        // Kd_490 is the third (2) wavelength
        return computeKdAtIndex(2);
    }

    private double computeKdAtIndex(int i) {
        double a_tot_mer8 = computeA_tot_mer8(i, aPig, aGelbstoff, aBtsm);
        double bb_tot_mer8 = computeBb_tot_mer8(i, bTsm);
        return Math.sqrt(a_tot_mer8 * (a_tot_mer8 + 2.0 * bb_tot_mer8));
    }

    private double computeBb_tot_mer8(int index, double bTsm) {
        return 0.5 * getB_wat_mer8()[index] + 0.05 * bTsm * getB_tsm_mer8()[index];
    }

    private double computeA_tot_mer8(int index, double aPig, double aGelbstoff, double aBtsm) {
        return getA_wat_mer8()[index] + aPig * getA_pig_mer8()[index] + aGelbstoff * getA_gelb_mer8()[index] + aBtsm * getA_btsm_mer8()[index];
    }

}
