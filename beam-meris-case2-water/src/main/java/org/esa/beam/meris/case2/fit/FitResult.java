package org.esa.beam.meris.case2.fit;
/*
 * Created on 12.09.2006
 * @author Schiller
 */

import Jama.Matrix;

public class FitResult {

    public int niter;
    public double ChiSq;
    public double startChiSq;
    public Matrix Jacobian;
    public Matrix CovPars;
    public String returnReason;
    public double[] parsfit;
    public double[] startModelRes;
    public double[] finalModelRes;

}
