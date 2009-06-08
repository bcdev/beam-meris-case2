package org.esa.beam.case2.util.nn;


import org.esa.beam.case2.util.FormattedReader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

/**
 * This class is for using a Neural Net (NN) of type ffbp in a Java program.
 * The program for training such a NN "ffbp1.0" was written in C by @author H.Schiller.
 * You can get this program (including documentation)   <a href="http://gfesun1.gkss.de/software/ffbp/">here</a> .
 * The class only works for NN's (i.e. ".net"-files) generated with "ffbp1.0".
 *
 * @author H. Schiller modified by K.Schiller
 *         Copyright GKSS/KOF
 *         Created on 04.11.2003
 */
public class NNffbpAlphaTab {
    /**
     * The number of input variables to the NN.
     */
    public int nn_in;
    /**
     * The number of output variables of the NN.
     */
    public int nn_out;
    /**
     * The vector contains the smallest value for each input varible to the NN seen during the training phase.
     */
    public double[] inmin;
    /**
     * The vector contains the biggest value for each input varible to the NN seen during the training phase.
     */
    public double[] inmax;
    /**
     * The vector contains the smallest value for each output varible to the NN seen during the training phase.
     */
    public double[] outmin;
    /**
     * The vector contains the biggest value for each output varible to the NN seen during the training phase.
     */
    public double[] outmax;


    /**
     * The number of planes of the NN.
     */
    private int nplanes;
    /**
     * A vector of length {@link #nplanes} containing the number of neurons in each plane.
     */
    private int[] size;
    /**
     * Contains the weight ("connection strength") between each pair of neurons when going from ine plane to the next.
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
     * The vector contains the names of the {@link #nn_in} input varibales.
     */
    private String[] names_varin;
    /**
     * The vector contains the names of the {@link #nn_out} output varibales.
     */
    private String[] names_varout;
    /**
     * Specifies the length of the table containing the tabulated activation function.
     */
    private int nAlpha = 100000;
    /**
     * The table containing the tabulated activation function as used during the training of the NN.
     */
    private double[] alphaTab = new double[nAlpha];
    /**
     * Specifies the cutting of the activation function.
     * For values below alphaStart alphaTab[0] is used; for values greater
     * (-alphaStart) alphaTab[nAlpha - 1] is used.
     */
    private double alphaStart = -10.0;
    /**
     * The reciprocal of the increment of the entries of {@link #alphaTab}.
     */
    private double recDeltaAlpha;

    /**
     * From knowlegde of the netname the NN is read by calling {@link #net_read}.
     * The netname is set; the {@link #alphaTab} is calculated.
     *
     * @param netname The String containing the name of the NN to be used.
     */
    public NNffbpAlphaTab(String netname) throws IOException {
        net_read(netname);
        makeAlphaTab();
    }

    /**
     * Method makeAlphaTab
     * When an instance of this class is initialized this method is called and fills the
     * {@link #alphaTab} with the activation function used during the training of the NN.
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
     * Method net_read The contents of ".net"-file is read from this file and all the weights, biases, etc are set accordingly.
     *
     * @param netname A String containing the filename of the ".net"-file.
     * @return If everything worked fine (i.e. everything needed was read) it returns "true", otherwise "false" is returned.
     */
    boolean net_read(String netname) throws IOException {
        boolean res = false;
        String line;
        double[] h = new double[2];
        RandomAccessFile in = new RandomAccessFile(netname, "r");
        long fileanfang = in.getFilePointer();
        FormattedReader inf = new FormattedReader(in);
        inf.noComments();
        char ch = '0';
        while (ch != '#') ch = (char) in.read();
        line = inf.rString();			//read the rest of the line which has the #
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
        while (ch != '=') ch = (char) in.read();
        long pos = in.getFilePointer();
        this.nplanes = (int) inf.rlong();
        in.seek(pos);
        long[] hh = inf.rlong(this.nplanes + 1);
        this.size = new int[this.nplanes];
        for (int i = 0; i < this.nplanes; i++) this.size[i] = (int) hh[i + 1];
        this.wgt = new double[this.nplanes - 1][][];
        for (int i = 0; i < this.nplanes - 1; i++) this.wgt[i] = new double[this.size[i + 1]][this.size[i]];
        this.bias = new double[this.nplanes - 1][];
        for (int i = 0; i < this.nplanes - 1; i++) this.bias[i] = new double[this.size[i + 1]];
        this.act = new double[this.nplanes][];
        for (int i = 0; i < this.nplanes; i++) this.act[i] = new double[this.size[i]];
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            line = inf.rString();
            for (int i = 0; i < this.size[pl + 1]; i++) this.bias[pl][i] = inf.rdouble();
        }
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            line = inf.rString();
            for (int i = 0; i < this.size[pl + 1]; i++) {
                for (int j = 0; j < this.size[pl]; j++) this.wgt[pl][i][j] = inf.rdouble();
            }
        }
        in.seek(fileanfang);
        for (int i = 0; i < 9; i++) inf.rString();
        this.names_varin = new String[nn_in];
        for (int i = 0; i < nn_in; i++) {
            line = inf.rString();
            StringTokenizer st = new StringTokenizer(line);
            for (int k = 0; k < 4; k++) names_varin[i] = st.nextToken();
        }
        for (int i = 0; i < 2; i++) inf.rString();
        this.names_varout = new String[nn_out];
        for (int i = 0; i < nn_out; i++) {
            line = inf.rString();
            StringTokenizer st = new StringTokenizer(line);
            for (int k = 0; k < 4; k++) names_varout[i] = st.nextToken();
        }

        if (in != null) in.close();
        res = true;
        return res;
    }

    /**
     * Method activation The output signal is found by consulting {@link #alphaTab}
     * for the index associated with incoming signal x.
     *
     * @param x The signal incoming to the neuron for which the response is calculated.
     * @return The output signal.
     */
    private double activation(double x) {
        int index = (int) ((x - this.alphaStart) * this.recDeltaAlpha);
        if (index < 0) index = 0;
        if (index >= this.nAlpha) index = this.nAlpha - 1;

        return this.alphaTab[index];
    }

    /**
     * Method scp The scalar product of two vectors (same lengths) is calculated.
     *
     * @param x The first vector.
     * @param y The second vector.
     * @return The scalar product of these two vector.
     */
    private static double scp(double[] x, double[] y) {
        double sum = 0.0;
        for (int i = 0; i < x.length; i++) sum += x[i] * y[i];
        return sum;
    }

    /**
     * Method diag2Matrix For a given vector a square matrix of the same size is calculated
     * containing the vector as main diagonal and zeros elsewhere.
     *
     * @param diag The vector containing the diagonal elements of the resultant matrix.
     * @return The diagonal matrix.
     */
    private static double[][] diag2Matrix(double[] diag) {
        int dim = diag.length;
        double[][] res = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (j == i)
                    res[i][i] = diag[i];
                else
                    res[i][j] = 0.0;
            }
        }
        return res;
    }

    /**
     * Method matrixTimesMatrix Multiplies two matrices (m1*m2) and returns the result.
     *
     * @param m1 The first matrix (its number of columns must be the same as the number of rows of m2).
     * @param m2 The second matrix.
     * @return The resultant matrix of size [m1Rows, m2Columns].
     */
    private static double[][] matrixTimesMatrix(double[][] m1, double[][] m2) {
        int m1Rows = m1.length;
        int m1Columns = m1[0].length;
        int m2Rows = m2.length;
        int m2Columns = m2[0].length;
        if (m1Columns != m2Rows) {
            throw new IllegalArgumentException("m1Columns != m2Rows: m1 #columns " + m1Columns + ", m2 #rows: " + m2Rows);
        }
        double[][] res = new double[m1Rows][m2Columns];
        for (int i = 0; i < m1Rows; i++) {
            for (int j = 0; j < m2Columns; j++) {
                double sum = 0.0;
                for (int k = 0; k < m2Rows; k++) {
                    sum += m1[i][k] * m2[k][j];
                }
                res[i][j] = sum;
            }
        }
        return res;
    }

    /**
     * Method calcJacobi The NN is used. For a given input vector the corresponding
     * output vector together with the corresponding Jacobi matrix is returned as an
     * instance of class {@link NNCalc}.
     *
     * @param nnInp The vector contains the {@link #nn_in} input parameters (must be in right order).
     * @return The output  and corresponding Jacobi matrix of the NN.
     */
    public NNCalc calcJacobi(double[] nnInp) {
        NNCalc res = new NNCalc();
        res.nnOutput = new double[this.nn_out];

        double[] dADX = new double[this.nn_in];
        for (int i = 0; i < this.nn_in; i++) {
            this.act[0][i] = (nnInp[i] - this.inmin[i]) / (this.inmax[i] - this.inmin[i]);
            dADX[i] = 1.0 / (this.inmax[i] - this.inmin[i]);
        }
        double[][] dActDX = diag2Matrix(dADX);
        for (int pl = 0; pl < this.nplanes - 1; pl++) {
            double[] help = new double[this.size[pl + 1]];
            for (int i = 0; i < this.size[pl + 1]; i++) {
                this.act[pl + 1][i] = activation(this.bias[pl][i] + scp(this.wgt[pl][i], this.act[pl]));
                help[i] = this.act[pl + 1][i] * (1.0 - this.act[pl + 1][i]);
            }
            dActDX = matrixTimesMatrix(diag2Matrix(help), matrixTimesMatrix(this.wgt[pl], dActDX));
        }

        double[] help2 = new double[this.nn_out];
        for (int i = 0; i < nn_out; i++) {
            res.nnOutput[i] = this.act[this.nplanes - 1][i] * (this.outmax[i] - this.outmin[i]) + this.outmin[i];
            help2[i] = this.outmax[i] - this.outmin[i];
        }
        res.jacobiMatrix = matrixTimesMatrix(diag2Matrix(help2), dActDX);
        return res;
    }

    /**
     * Method calc The NN is used. For a given input vector the corresponding
     * output vector is returned.
     *
     * @param nninp The vector contains the {@link #nn_in} input parameters (must be in right order).
     * @return The  {@link #nn_out}-long output vector.
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

}


