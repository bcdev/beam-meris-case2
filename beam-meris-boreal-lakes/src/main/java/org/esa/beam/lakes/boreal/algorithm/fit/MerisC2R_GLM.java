package org.esa.beam.lakes.boreal.algorithm.fit;

import org.esa.beam.case2.algorithm.fit.ModelInterf4LM;
import org.esa.beam.case2.util.nn.NNCalc;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;

/**
 * @author Schiller
 *
 * Created on 12.09.2006
 */
public class MerisC2R_GLM implements ModelInterf4LM {
	private GenLM myLM;
	private NNffbpAlphaTabFast forwNN;
	private double[] nnIn = new double[10];
    private int npars=3;
	private int nmeas=7;
    private double wlVariance;

    @Override
    public void initSetOfFits(Object forwNetName, double waterReflLogVariance) {

        forwNN = (NNffbpAlphaTabFast)(forwNetName);
        wlVariance = waterReflLogVariance;

        myLM = new GenLM(this);
        myLM.nitermax = 200;
        myLM.nu = 2;
        myLM.tau  = 1.0e-4;// sehr klein (e-6), wenn gute startwerte
        myLM.eps1 = 1.0e-6;
        myLM.eps2 = 1.0e-16;

        // setting stuff fixed due to dimensions of Neural Net
        myLM.setNmeasNpars(nmeas, npars);

    }

    public GenLM getMyLM() {
        return myLM;
    }

	@Override
    public void initSingleFit(Object initValues) {
		ChiSquareFitGLM.Data4SingleFitInitialization myIni = (ChiSquareFitGLM.Data4SingleFitInitialization)(initValues);


        nnIn[0]  = myIni.theta_sun_grad;
        nnIn[1]  = myIni.theta_view_grad;
        nnIn[2]  = myIni.azi_diff_grad;

		myLM.startPars[0] = myIni.ln_b_SPM_b_White;
		myLM.startPars[1] = myIni.ln_a_Chlor;
		myLM.startPars[2] = myIni.ln_a_Yellow_a_SPM;

		for (int i = 0; i < nmeas; i++){
			myLM.measurements[i] = myIni.wlRefl[i];
			for (int j = 0; j < nmeas; j++){
				myLM.CovMeas.set(i, j, 0);
			}
		}
		myLM.CovMeas.set(0, 0,   wlVariance);
		myLM.CovMeas.set(1, 1,   wlVariance);
		myLM.CovMeas.set(2, 2,   wlVariance);
		myLM.CovMeas.set(3, 3,   wlVariance);
		myLM.CovMeas.set(4, 4,   wlVariance);
		myLM.CovMeas.set(5, 5,   wlVariance);
		myLM.CovMeas.set(6, 6,   wlVariance);
//		myLM.CovMeas.set(7, 7,   wlVariance);

//		>> x=[0.9 0.95 0.99];
//		>> cut=chi2inv(x,5)
//		cut =
//		9.2364        11.07       15.086

	}

	@Override
    public void modelAndJacobian(double[] pars) {
        boolean amPoller = false;
        final double[] inmin = forwNN.getInmin();
        final double[] inmax = forwNN.getInmax();
        for (int i = 0; i < npars; i++){
            // check if pars are in bounds of NN
            if (pars[i] < inmin[i + 3]) {
                pars[i] = inmin[i + 3];
                amPoller = true;
            }
            if (pars[i] > inmax[i + 3]) {
                pars[i] = inmax[i + 3];
                amPoller = true;
            }
            nnIn[i + 3] = pars[i];
			//System.out.println("pars= "+pars[i]);
		}
		// since pars might have changed:
		if(amPoller) {
            System.arraycopy(pars, 0, myLM.newpars, 0, npars);
		}
        NNCalc nnRes = forwNN.calcJacobi(nnIn);
		myLM.modelRes = nnRes.getNnOutput();

        final double[][] jacobiMatrix = nnRes.getJacobiMatrix();
		for (int i = 0; i < nmeas; i++){
			//System.out.println("modres= " + myLM.finalModelRes[i]);
			for (int j = 0; j < npars; j++){
                myLM.Jacobian.set(i, j, jacobiMatrix[i][j + 3]);
			}
		}
	}
}
