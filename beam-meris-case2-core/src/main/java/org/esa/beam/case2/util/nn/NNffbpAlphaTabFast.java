package org.esa.beam.case2.util.nn;


import org.esa.beam.case2.util.FormattedReader;
import org.esa.beam.case2.util.FormattedStringReader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.StringTokenizer;

/**
 * This class is for using a Neural Net (NN) of type ffbp in a Java program. The
 * program for training such a NN "ffbp1.0" was written in C by
 *
 * @author H.Schiller. You can get this program (including documentation) <a
 *         href="http://gfesun1.gkss.de/software/ffbp/">here </a>. The class
 *         only works for NN's (i.e. ".net"-files) generated with "ffbp1.0".
 * @author H. Schiller modified by K.Schiller Copyright GKSS/KOF Created on
 *         04.11.2003
 */
public class NNffbpAlphaTabFast {

    /**
     * The vector contains the smallest value for each input varible to the NN
     * seen during the training phase.
     */
    public double[] inmin;
    /**
     * The vector contains the biggest value for each input varible to the NN
     * seen during the training phase.
     */
    public double[] inmax;
    /**
     * The vector contains the smallest value for each output varible to the NN
     * seen during the training phase.
     */
    public double[] outmin;
    /**
     * The vector contains the biggest value for each output varible to the NN
     * seen during the training phase.
     */
    public double[] outmax;
    /**
     * The number of planes of the NN.
     */
    private int nplanes;
    /**
     * A vector of length {@link #nplanes}containing the number of neurons in
     * each plane.
     */
    private int[] size;
    /**
     * Contains the weight ("connection strength") between each pair of neurons
     * when going from ine plane to the next.
     */
    private double[][][] wgt;
    /**
     * A matrix containing the biases for each neuron in each plane.
     */
    private double[][] bias;
    /**
     * A matrix containing the activation signal of each neuron in each plane.
     */
    private double[][] act;
    /**
     * The number of input variables to the NN.
     */
    private int nn_in;
    /**
     * The number of output variables of the NN.
     */
    private int nn_out;
    /**
     * The vector contains the names of the {@link #nn_in}input varibales.
     */
    private String[] names_varin;
    /**
     * The vector contains the names of the {@link #nn_out}output varibales.
     */
    private String[] names_varout;
    /**
     * A String containing the name of the NN used.
     */
    private String netname;
    /**
     * Specifies the length of the table containing the tabulated activation
     * function.
     */
    private int nAlpha = 100000;
    /**
     * The table containing the tabulated activation function as used during the
     * training of the NN.
     */
    private double[] alphaTab = new double[nAlpha];
    /**
     * Specifies the cutting of the activation function. For values below
     * alphaStart alphaTab[0] is used; for values greater (-alphaStart)
     * alphaTab[nAlpha - 1] is used.
     */
    private double alphaStart = -10.0;
    /**
     * The reciprocal of the increment of the entries of {@link #alphaTab}.
     */
    private double recDeltaAlpha;

    /**
     * From knowlegde of the netname the NN is read by calling {@link #net_read}.
     * The netname is set; the {@link #alphaTab}is calculated.
     *
     * @param netname The String containing the name of the NN to be used.
     */
    public NNffbpAlphaTabFast(String netname) throws
                                              IOException {
        net_read(netname);
        this.netname = netname;
        makeAlphaTab();
        NNresjacob = new NNCalc();
        declareArrays();
    }

    /**
     * From knowlegde of the netname the NN is read by calling
     * {@link #netReadFromString}. The netname is set; the {@link #alphaTab}is
     * calculated.
     *
     * @param netname The String containing the name of the NN to be used.
     * @param net     The String containing the contents of the ".net"-file.
     */
    public NNffbpAlphaTabFast(String netname, String net) throws
                                                          IOException {
        netReadFromString(net);
        this.netname = netname;
        makeAlphaTab();
        NNresjacob = new NNCalc();
        declareArrays();
    }

    /**
     * Method makeAlphaTab When an instance of this class is initialized this
     * method is called and fills the {@link #alphaTab}with the activation
     * function used during the training of the NN.
     */
    private void makeAlphaTab() {
        double delta = -2.0 * this.alphaStart / (nAlpha - 1.0);
        double sum = this.alphaStart + 0.5 * delta;
        for (int i = 0; i < nAlpha; i++) {
            this.alphaTab[i] = 1.0 / (1.0 + Math.exp(-sum));
            sum += delta;
        }
        this.recDeltaAlpha = 1.0 / delta;
    }

    /**
     * Method netReadFromString The contents of ".net"-file is read from a
     * String and all the weights, biases, etc are set accordingly.
     *
     * @param net The String containing the contents of the ".net"-file.
     *
     * @return If everything worked fine (i.e. everything needed was read) it
     *         returns "true", otherwise "false" is returned.
     */
    boolean netReadFromString(String net) throws
                                          IOException {
        boolean res;
        String line;
        double[] h = new double[2];
        StringReader in = new StringReader(net);
        FormattedStringReader inf = new FormattedStringReader(in);
        inf.noComments();
        char ch = '0';
        while (ch != '#') {
            ch = (char) in.read();
        }
        line = inf.rString();            //read the rest of the line which
        // has the #
        this.nn_in = (int) inf.rlong();
        this.inmin = new double[nn_in];
        this.inmax = new double[nn_in];
        for (int i = 0; i < nn_in; i++) {
            h = inf.rdouble(2);
            this.inmin[i] = h[0];
            this.inmax[i] = h[1];
        }
        this.nn_out = (int) inf.rlong();
        this.outmin = new double[nn_out];
        this.outmax = new double[nn_out];
        for (int i = 0; i < nn_out; i++) {
            h = inf.rdouble(2);
            this.outmin[i] = h[0];
            this.outmax[i] = h[1];
        }
        while (ch != '=') {
            ch = (char) in.read();
        }
        in.mark(1000000);
        this.nplanes = (int) inf.rlong();
        in.reset();
        long[] hh = inf.rlong(this.nplanes + 1);
        this.size = new int[this.nplanes];
        for (int i = 0; i < this.nplanes; i++) {
            this.size[i] = (int) hh[i + 1];
        }
        this.wgt = new double[this.nplanes - 1][][];
        for (int i = 0; i < this.nplanes - 1; i++) {
            this.wgt[i] = new double[this.size[i + 1]][this.size[i]];
        }
        this.bias = new double[this.nplanes - 1][];
        for (int i = 0; i < this.nplanes - 1; i++) {
            this.bias[i] = new double[this.size[i + 1]];
        }
        this.act = new double[this.nplanes][];
        for (int i = 0; i < this.nplanes; i++) {
            this.act[i] = new double[this.size[i]];
        }
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            line = inf.rString();
            for (int i = 0; i < this.size[pl + 1]; i++) {
                this.bias[pl][i] = inf.rdouble();
            }

        }
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            line = inf.rString();
            for (int i = 0; i < this.size[pl + 1]; i++) {
                for (int j = 0; j < this.size[pl]; j++) {
                    this.wgt[pl][i][j] = inf.rdouble();
                }
            }
        }
        in.close();
        StringReader in2 = new StringReader(net);
        FormattedStringReader inf2 = new FormattedStringReader(in2);
        inf2.noComments();
        for (int i = 0; i < 9; i++) {
            inf2.rString();
        }
        this.names_varin = new String[nn_in];
        for (int i = 0; i < nn_in; i++) {
            line = inf2.rString();
            StringTokenizer st = new StringTokenizer(line);
            for (int k = 0; k < 4; k++) {
                names_varin[i] = st.nextToken();
            }
        }
        for (int i = 0; i < 2; i++) {
            inf2.rString();
        }
        this.names_varout = new String[nn_out];
        for (int i = 0; i < nn_out; i++) {
            line = inf2.rString();
            StringTokenizer st = new StringTokenizer(line);
            for (int k = 0; k < 4; k++) {
                names_varout[i] = st.nextToken();
            }
        }

        if (in2 != null) {
            in2.close();
        }
        res = true;

        return res;
    }

    /**
     * Method net_read The contents of ".net"-file is read from this file and
     * all the weights, biases, etc are set accordingly.
     *
     * @param netname A String containing the filename of the ".net"-file.
     *
     * @return If everything worked fine (i.e. everything needed was read) it
     *         returns "true", otherwise "false" is returned.
     */
    boolean net_read(String netname) throws
                                     IOException {
        boolean res = false;
        File fr = new File(netname);
        String line;
        double[] h = new double[2];
        RandomAccessFile in = new RandomAccessFile(netname, "r");
        long fileanfang = in.getFilePointer();
        FormattedReader inf = new FormattedReader(in);
        inf.noComments();
        char ch = '0';
        while (ch != '#') {
            ch = (char) in.read();
        }
        line = inf.rString();            //read the rest of the line
        // which has the #
        this.nn_in = (int) inf.rlong();
        this.inmin = new double[nn_in];
        this.inmax = new double[nn_in];
        for (int i = 0; i < nn_in; i++) {
            h = inf.rdouble(2);
            this.inmin[i] = h[0];
            this.inmax[i] = h[1];
        }
        this.nn_out = (int) inf.rlong();
        this.outmin = new double[nn_out];
        this.outmax = new double[nn_out];
        for (int i = 0; i < nn_out; i++) {
            h = inf.rdouble(2);
            this.outmin[i] = h[0];
            this.outmax[i] = h[1];
        }
        while (ch != '=') {
            ch = (char) in.read();
        }
        long pos = in.getFilePointer();
        this.nplanes = (int) inf.rlong();
        in.seek(pos);
        long[] hh = inf.rlong(this.nplanes + 1);
        this.size = new int[this.nplanes];
        for (int i = 0; i < this.nplanes; i++) {
            this.size[i] = (int) hh[i + 1];
        }
        this.wgt = new double[this.nplanes - 1][][];
        for (int i = 0; i < this.nplanes - 1; i++) {
            this.wgt[i] = new double[this.size[i + 1]][this.size[i]];
        }
        this.bias = new double[this.nplanes - 1][];
        for (int i = 0; i < this.nplanes - 1; i++) {
            this.bias[i] = new double[this.size[i + 1]];
        }
        this.act = new double[this.nplanes][];
        for (int i = 0; i < this.nplanes; i++) {
            this.act[i] = new double[this.size[i]];
        }
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            line = inf.rString();
            for (int i = 0; i < this.size[pl + 1]; i++) {
                this.bias[pl][i] = inf.rdouble();
            }
        }
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            line = inf.rString();
            for (int i = 0; i < this.size[pl + 1]; i++) {
                for (int j = 0; j < this.size[pl]; j++) {
                    this.wgt[pl][i][j] = inf.rdouble();
                }
            }
        }
        in.seek(fileanfang);
        for (int i = 0; i < 9; i++) {
            inf.rString();
        }
        this.names_varin = new String[nn_in];
        for (int i = 0; i < nn_in; i++) {
            line = inf.rString();
            StringTokenizer st = new StringTokenizer(line);
            for (int k = 0; k < 4; k++) {
                names_varin[i] = st.nextToken();
            }
        }
        for (int i = 0; i < 2; i++) {
            inf.rString();
        }
        this.names_varout = new String[nn_out];
        for (int i = 0; i < nn_out; i++) {
            line = inf.rString();
            StringTokenizer st = new StringTokenizer(line);
            for (int k = 0; k < 4; k++) {
                names_varout[i] = st.nextToken();
            }
        }

        if (in != null) {
            in.close();
        }
        res = true;
        return res;
    }

    /**
     * Method activation The output signal is found by consulting
     * {@link #alphaTab}for the index associated with incoming signal x.
     *
     * @param x The signal incoming to the neuron for which the response is
     *          calculated.
     *
     * @return The output signal.
     */
    private double activation(double x) {
        int index = (int) ((x - this.alphaStart) * this.recDeltaAlpha);
        if (index < 0) {
            index = 0;
        }
        if (index >= this.nAlpha) {
            index = this.nAlpha - 1;
        }

        return this.alphaTab[index];
    }

    /**
     * Method scp The scalar product of two vectors (same lengths) is
     * calculated.
     *
     * @param x The first vector.
     * @param y The second vector.
     *
     * @return The scalar product of these two vector.
     */
    private double scp(double[] x, double[] y) {
        double sum = 0.;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * y[i];
        }
        return sum;
    }


    double[][][] dActDX;
    double[][] help;
    private NNCalc NNresjacob;

    /**
     * Method calcJacobi The NN is used. For a given input vector the
     * corresponding output vector together with the corresponding Jacobi matrix
     * is returned as an instance of class {@link NNCalc}.
     *
     * @param nnInp The vector contains the {@link #nn_in}input parameters (must
     *              be in right order).
     *
     * @return The output and corresponding Jacobi matrix of the NN.
     */
    public NNCalc calcJacobi(double[] nnInp) {

        final NNCalc res = NNresjacob;

        for (int i = 0; i < nn_in; i++) {
            act[0][i] = (nnInp[i] - inmin[i]) / (inmax[i] - inmin[i]);
        }

        for (int pl = 0; pl < nplanes - 1; pl++) {
            final double[] act_pl_1 = act[pl + 1];
            final double[] help_pl = help[pl];
            final double[][] wgt_pl = wgt[pl];
            final double[] bias_pl = bias[pl];
            final double[] act_pl = act[pl];

            for (int i = 0; i < size[pl + 1]; i++) {
                act_pl_1[i] = activation(bias_pl[i] + scp(wgt_pl[i], act_pl));
                help_pl[i] = act_pl_1[i] * (1.0 - act_pl_1[i]);
            }

            double sum;
            final double[][] dActDX_pl = dActDX[pl];

            for (int i = 0; i < size[pl + 1]; i++) {
                for (int j = 0; j < nn_in; j++) {
                    sum = 0.;
                    final double help_pl_i = help_pl[i];
                    final double[] wgt_pl_i = wgt_pl[i];

                    for (int k = 0; k < size[pl]; k++) {
                        sum += help_pl_i * wgt_pl_i[k] * dActDX_pl[k][j];
                    }

                    dActDX[pl + 1][i][j] = sum;
                }
            }
        }


        final double[] act_nplanes_1 = act[nplanes - 1];
        final double[][] dActDX_nplanes_1 = dActDX[nplanes - 1];

        for (int i = 0; i < nn_out; i++) {
            final double diff = outmax[i] - outmin[i];
            res.nnOutput[i] = act_nplanes_1[i] * diff + outmin[i];
            final double[] res_jacobiMatrix_i = res.jacobiMatrix[i];
            final double[] dActDX_nplanes_1_i = dActDX_nplanes_1[i];
            for (int k = 0; k < nn_in; k++) {
                res_jacobiMatrix_i[k] = dActDX_nplanes_1_i[k] * diff;
            }
        }
        return res;

// This is the not optimized version (original) compared to the above.
// Keep this until it is aproved that functionality is the same.

//        NNCalc res = this.NNresjacob;
//
//        for (int i = 0; i < this.nn_in; i++) {
//            this.act[0][i] = (nnInp[i] - this.inmin[i]) / (this.inmax[i] - this.inmin[i]);
//        }
//        for (int pl = 0; pl < this.nplanes - 1; pl++) {
//            for (int i = 0; i < this.size[pl + 1]; i++) {
//                this.act[pl + 1][i] = activation(this.bias[pl][i] + scp(this.wgt[pl][i], this.act[pl]));
//                help[pl][i] = this.act[pl + 1][i] * (1.0 - this.act[pl + 1][i]);
//            }
////dActDX[pl+1] = this.matrixTimesMatrix(this.diag2Matrix(help), this.matrixTimesMatrix(this.wgt[pl], dActDX));
//            for (int i = 0; i < this.size[pl + 1]; i++) {
//                for (int j = 0; j < this.nn_in; j++) {
//                    double sum = 0.;
//                    for (int k = 0; k < this.size[pl]; k++)
//                        sum += help[pl][i] * this.wgt[pl][i][k] * dActDX[pl][k][j];
//                    dActDX[pl + 1][i][j] = sum;
//                }
//            }
//        }
//
//
//        for (int i = 0; i < nn_out; i++) {
//            res.nnOutput[i] = this.act[this.nplanes - 1][i] * (this.outmax[i] - this.outmin[i]) + this.outmin[i];
//            double diff = this.outmax[i] - this.outmin[i];
//            for (int k = 0; k < this.nn_in; k++) res.jacobiMatrix[i][k] = dActDX[this.nplanes - 1][i][k] * diff;
//        }
////		res.jacobiMatrix = this.matrixTimesMatrix(this.diag2Matrix(help2), dActDX[this.nplanes]);
//        return res;
    }

    public void declareArrays() {
        NNresjacob.nnOutput = new double[this.nn_out];
        NNresjacob.jacobiMatrix = new double[this.nn_out][this.nn_in];
//        System.out.println(this.nn_in);
        dActDX = new double[this.nplanes][][];
        dActDX[0] = new double[this.nn_in][this.nn_in];
        for (int i = 0; i < this.nn_in; i++) {
            for (int j = 0; j < this.nn_in; j++) {
                dActDX[0][i][j] = 0.;
            }
            dActDX[0][i][i] = 1.0 / (this.inmax[i] - this.inmin[i]);
            ;
        }
        help = new double[this.nplanes - 1][];
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            help[pl] = new double[this.size[pl + 1]];
            dActDX[pl + 1] = new double[this.size[pl + 1]][this.nn_in];
//System.out.println()
        }
    }

    /**
     * Method calc The NN is used. For a given input vector the corresponding
     * output vector is returned.
     *
     * @param nninp The vector contains the {@link #nn_in}input parameters (must
     *              be in right order).
     *
     * @return The {@link #nn_out}-long output vector.
     */
    public double[] calc(double[] nninp) {
        double[] res = new double[nn_out];

        for (int i = 0; i < nn_in; i++) {
            this.act[0][i] = (nninp[i] - this.inmin[i]) / (this.inmax[i] - this.inmin[i]);
        }
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            for (int i = 0; i < this.size[pl + 1]; i++) {
                this.act[pl + 1][i] = activation(this.bias[pl][i] + scp(this.wgt[pl][i], this.act[pl]));
            }
        }
        for (int i = 0; i < nn_out; i++) {
            res[i] = this.act[this.nplanes - 1][i] * (this.outmax[i] - this.outmin[i]) + this.outmin[i];
        }
        return res;
    }


    static void procPix(NNffbpAlphaTabFast invNN, double[] refl, double sz,
                        double sa, double vz, double va) {
        double[] nn1in = new double[11];
        double[] start = new double[3];
        nn1in[0] = sz;
        nn1in[1] = vz;
        double ad = sa - va;
        if (ad > 180.0) {
            ad = 360.0 - ad;
        }
        nn1in[2] = ad;

        for (int i = 0; i < 8; i++) {
            if (refl[i] < 0.003) {
                refl[i] = 0.003;
            }
            nn1in[3 + i] = Math.log(refl[i] / Math.PI);
        }
        start = invNN.calc(nn1in);
        System.out.println(start[0] + "   " + start[1] + "   " + start[2]);
        NNCalc jac = invNN.calcJacobi(nn1in);
        System.out.println(jac.nnOutput[0] + "   " + jac.nnOutput[1] + "   "
                           + jac.nnOutput[2]);
        System.out.println();
        for (int i = 0; i < 11; i++) {
            System.out.println(jac.jacobiMatrix[0][i] + "   " + jac.jacobiMatrix[1][i] + "   "
                               + jac.jacobiMatrix[2][i]);
        }

    }

    /**
     * 3 test cases. Vor der Umstellung wurden diese Pflichtresultate erhalten:
     * <p/>
     * -0.4349655820776164   -2.2005421482716496   -3.2723717634871043
     * -0.4349655820776164   -2.2005421482716496   -3.2723717634871043
     * <p/>
     * 0.0010000326788035746   0.0023864693148120644   0.0014226126722460411
     * -0.004406374463782042   -0.0011363018022542072   -3.352134729913523E-4
     * -8.546273458217443E-5   -3.1735906614017615E-4   -9.668678602307795E-4
     * 0.8005341591010349   2.4151849867796735   -5.743211625186681
     * 0.3169768083451105   -3.6843079076150156   9.753503687082057
     * -1.7798006344076476   -0.8053472925227223   -3.5813015314232133
     * -0.18508499454737876   0.2866928477738667   -0.06094682874759885
     * 0.16124552930062278   -0.30143064426847743   -1.6767697399754231
     * 2.1726081530457475   1.9316028423859082   2.58758566787883
     * 3.7901059043958902   3.0192540320058594   7.094628498220249
     * 2.883703268141097   3.499472228679545   2.6103803858676384
     * --------------------------------
     * -0.4349655820776164   -2.200900535924963   -3.2727222865355006
     * -0.4349655820776164   -2.200900535924963   -3.2727222865355006
     * <p/>
     * 9.97384275226042E-4   0.002386342343774813   0.0014232631582410645
     * -0.004414083682982182   -0.0011378845865238115   -3.360888992917062E-4
     * -8.387674322436887E-5   -3.1695203627945086E-4   -9.657786573932688E-4
     * 0.800403990607393   2.414566044374777   -5.7422423266028675
     * 0.3171867015461535   -3.683627466861529   9.751655734048212
     * -1.7801489505814498   -0.8056723008098474   -3.5810142945300227
     * -0.1854042716648723   0.28625302220881477   -0.0612092069386645
     * 0.16223627325640277   -0.3003619487559863   -1.6754993977205006
     * 2.172732818776771   1.9314831155710601   2.587297976994376
     * 3.790793393277022   3.019746165856709   7.095180311779035
     * 2.8835495262141473   3.4992685349283725   2.610344492997593
     * --------------------------------
     * -0.22840453647604875   -1.9940100438099178   -3.374314881617909
     * -0.22840453647604875   -1.9940100438099178   -3.374314881617909
     * <p/>
     * 0.0012953729448661503   0.002703526078646126   0.0014404408240268418
     * -0.00437525883855079   -0.001058055156360221   -2.150475924969742E-5
     * -1.6488620413454043E-4   -4.518781505035368E-4   -0.0010072890584228898
     * 0.8884760400830616   2.233799668035669   -5.419195570109496
     * 0.20261979993069665   -3.407175008217279   9.08843862360499
     * -1.483776248499747   -0.5393590335883345   -3.1699713685817414
     * 0.038374150363119704   0.5203637337774464   0.01281015122331096
     * -0.6747114754886835   -1.1451439241024164   -2.0957640485604245
     * 2.1528214762731763   1.9371271350064638   2.1714067254380045
     * 2.9354659324468697   2.597293720815094   5.378606050736217
     * 2.7666382687171045   3.20648447560949   2.1174390491087025
     */
    public static void main(String[] args) {
        NNffbpAlphaTabFast nn1 = null;
        try {
            nn1 = new NNffbpAlphaTabFast(
                    "C:/Projects/beam3-plugins/MerisC2RAlgo/src/org/esa/beam/lakes/util/nn/45x16x12x8x5_3716.7_invreflcutx3.net");
        } catch (IOException e) {

        }

        double[] refl = {0.015555037, 0.013211251, 0.012863345, 0.01253375,
                0.011050574, 0.0034332701, 0.0019317825, 9.979303E-4};
        procPix(nn1, refl, 34.11667, 132.99332, 34.94552, 100.71583);
        System.out.println("--------------------------------");
        double[] refl5 = {0.011618209, 0.009640641, 0.010428006, 0.010611114,
                0.0099153025, 0.0025909722, 0.001345836, 6.683355E-4};
        procPix(nn1, refl, 34.11861, 132.96034, 35.007233,
                100.704254);
        System.out.println("--------------------------------");
        double[] refl0 = {0.016781863, 0.013540846, 0.012973211, 0.012625305,
                0.011343547, 0.0036896218, 0.0020233365, 9.0637617E-4};
        procPix(nn1, refl0, 34.104694, 132.8511, 35.13029,
                100.67979);
    }
}


