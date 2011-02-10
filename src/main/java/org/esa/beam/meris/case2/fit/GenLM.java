package org.esa.beam.meris.case2.fit;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * @author Schiller
 */
public class GenLM {

    public ModelInterf4LM theFitproblem;

    public GenLM(ModelInterf4LM aFitproblem) {
        theFitproblem = aFitproblem;
    }


    public int nitermax;
    public double[] startPars;
    public double[] modelRes;
    public double[] measurements;
    public double[] pars;
    public double[] newpars;
    public Matrix ModErr;
    public Matrix CovMeas;
    public Matrix InvCovMeas;
    public Matrix Jacobian;
    public Matrix Gradient;
    public Matrix ParStep;
    private int npars, nmeas;

    public double mu, nu, tau, eps1, eps2;

    public void setNmeasNpars(int nmeas, int npars) {
        this.npars = npars;
        this.nmeas = nmeas;

        pars = new double[npars];
        newpars = new double[npars];
        Gradient = new Matrix(npars, 1);
        ParStep = new Matrix(npars, 1);
        startPars = new double[npars];
        modelRes = new double[nmeas];
        ModErr = new Matrix(nmeas, 1);
        measurements = new double[nmeas];
        modelRes = new double[nmeas];
        Jacobian = new Matrix(nmeas, npars);
        CovMeas = new Matrix(nmeas, nmeas);
    }


    public FitResult LMFit() {
        FitResult res = new FitResult();
        res.startModelRes = new double[nmeas];
        res.finalModelRes = new double[nmeas];
        InvCovMeas = CovMeas.inverse();
        res.returnReason = "skipped the while: gradient / eps1";
        res.niter = 0;
        System.arraycopy(startPars, 0, pars, 0, npars);
        theFitproblem.modelAndJacobian(pars);
        System.arraycopy(modelRes, 0, res.startModelRes, 0, nmeas);
        res.ChiSq = chiSq();
        res.startChiSq = res.ChiSq;
        Gradient = Jacobian.transpose().times(InvCovMeas.times(ModErr));  //28.11.06
        boolean finito = (Gradient.normInf() < eps1);
        res.CovPars = (Jacobian.transpose().times(InvCovMeas)).times(Jacobian);
        double mxdiag = Double.MIN_VALUE;
        for (int i = 0; i < npars; i++) {
            if (res.CovPars.get(i, i) > mxdiag) {
                mxdiag = res.CovPars.get(i, i);
            }
        }
        mu = tau * mxdiag;
        while (!finito && (res.niter < nitermax)) {
            res.returnReason = "mitermax reached ";
            res.niter++;
            //SingularValueDecomposition SVD = new SingularValueDecomposition(res.CovPars.plus(Matrix.identity(npars, npars).times(mu)));
            ParStep = (res.CovPars.plus(Matrix.identity(npars, npars).times(mu))).inverse().times(Gradient).uminus();
            if (ParStep.norm2() < eps2 * (new Matrix(pars, 1)).norm2() + eps2) {
                finito = true;
                res.returnReason = "small parameter step / eps2";
            } else {
                for (int i = 0; i < npars; i++) {
                    newpars[i] = pars[i] + ParStep.get(i, 0);
                }
                theFitproblem.modelAndJacobian(newpars);
                double newChiSq = chiSq();
                double rho = 2. * (res.ChiSq - newChiSq) / ParStep.transpose().times(
                        ParStep.times(mu).minus(Gradient)).get(0, 0);
                if (rho > 0.) {
                    System.arraycopy(newpars, 0, pars, 0, npars);
                    res.ChiSq = newChiSq;
                    res.CovPars = (Jacobian.transpose().times(InvCovMeas)).times(Jacobian);
                    Gradient = Jacobian.transpose().times(InvCovMeas.times(ModErr)); //28.11.06
                    finito = (Gradient.normInf() < eps1);
                    res.returnReason = "small gradient / eps1";
                    double mx = 0.33333;
                    double dh = (2. * rho - 1);
                    dh = 1. - dh * dh * dh;
                    if (dh > mx) {
                        mx = dh;
                    }
                    mu = mu * mx;
                    nu = 2.;
                    //System.out.println("  if  "+rho+"   mu="+mu+"   nu="+nu);
                } else {
                    //System.out.println("else  "+rho+"   mu="+mu+"   nu="+nu);
                    mu = mu * nu;
                    nu = 2. * nu;
                }
            }
        }
        if (res.niter == nitermax) {
            res.returnReason = "nitermax iterations done";
        }
        //res.CovPars.print(15, 10);
        res.CovPars = svdinv2(res.CovPars, 1.e-9);
        res.Jacobian = Jacobian.copy();
        res.parsfit = new double[npars];
        System.arraycopy(newpars, 0, res.parsfit, 0, npars);
        System.arraycopy(modelRes, 0, res.finalModelRes, 0, nmeas);
        //System.out.println(res.niter+" iters, "+res.returnReason);
        return res;
    }

    Matrix svdinv2(Matrix A, double eps) {
        Matrix U, S, V;
        SingularValueDecomposition myd = new SingularValueDecomposition(A);
        U = myd.getU();
        S = myd.getS();
        V = myd.getV();
        double[] sg = myd.getSingularValues();
        for (int i = 0; i < sg.length; i++) {
            if (sg[i] < eps) {
                sg[i] = 0.0;
            } else {
                sg[i] = 1.0 / sg[i];
            }
            S.set(i, i, sg[i]);
        }
        return V.times(S).times(U.transpose());
    }

    double chiSq() {
        for (int i = 0; i < nmeas; i++) {
            ModErr.set(i, 0, modelRes[i] - measurements[i]);
        }
        //System.out.println(ModErr.transpose().times(InvCovMeas).times(ModErr).get(0, 0));
        return ModErr.transpose().times(InvCovMeas).times(ModErr).get(0, 0);
    }

}
