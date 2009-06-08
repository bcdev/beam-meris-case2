package org.esa.beam.case2.util.nn;

/**
 * This class serves for defining a structure containing the output of a NN together
 * with the corresponding Jacobi matrix. An instance of this class is returned by the
 * {@link org.esa.beam.lakes.eutrophic.util.nn.NNffbpAlphaTabFast#calcJacobi(double[])} method.
 *
 * @author K. Schiller
 *         Copyright GKSS/KOF
 *         Created on 02.12.2000
 */
public class NNCalc {
    /**
     * The vector containing the output of a {@link org.esa.beam.lakes.eutrophic.util.nn.NNffbpAlphaTabFast}.
     */
    public double[] nnOutput;
    /**
     * The corresponding Jacobi matrix.
     */
    public double[][] jacobiMatrix;
}
