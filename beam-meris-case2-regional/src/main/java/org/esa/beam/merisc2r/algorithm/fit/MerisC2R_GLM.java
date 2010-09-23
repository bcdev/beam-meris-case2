package org.esa.beam.merisc2r.algorithm.fit;

import org.esa.beam.case2.algorithm.fit.ModelInterf4LM;
import org.esa.beam.case2.util.nn.NNCalc;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;

/*
* Created on 12.09.2006
*
* TODO To change the template for this generated file go to
* Window - Preferences - Java - Code Style - Code Templates
*/

/**
 * @author Schiller
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MerisC2R_GLM implements ModelInterf4LM {
	public GenLM myLM;
	public NNffbpAlphaTabFast forwNN;
	double[] nnIn = new double[11];
	NNCalc nnRes;
	int npars=3;
	int nmeas=8;
	public static boolean amPoller;
    private double wlVariance;

    public void initSetOfFits(Object forwNetName, double waterReflLogVariance) {

        forwNN = (NNffbpAlphaTabFast)(forwNetName);
        wlVariance = waterReflLogVariance;

        myLM = new GenLM(this);
        myLM.nitermax = 200;
        myLM.nu = 2;
        myLM.tau  = 1.e-4;// sehr klein (e-6), wenn gute startwerte
        myLM.eps1 = 1.e-6;
        myLM.eps2 = 1.e-16;

        // setting stuff fixed due to dimensions of Neural Net
        myLM.setNmeasNpars(nmeas, npars);

    }

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
		myLM.CovMeas.set(7, 7,   wlVariance);

//		>> x=[0.9 0.95 0.99];
//		>> cut=chi2inv(x,5)
//		cut =
//		9.2364        11.07       15.086

	}

	public void modelAndJacobian(double[] pars) {
		amPoller=false;
        for (int i = 0; i < npars; i++) {
            // check if pars are in bounds of NN
            if (pars[i] < forwNN.inmin[i + 3]) {
                pars[i] = forwNN.inmin[i + 3];
                amPoller = true;
            }
            if (pars[i] > forwNN.inmax[i + 3]) {
                pars[i] = forwNN.inmax[i + 3];
                amPoller = true;
            }
            nnIn[i + 3] = pars[i];
            //System.out.println("pars= "+pars[i]);
        }
        // since pars might have changed:
		if(amPoller) {
            System.arraycopy(pars, 0, myLM.newpars, 0, npars);
        }
        nnRes = forwNN.calcJacobi(nnIn);
		myLM.modelRes = nnRes.nnOutput;

		for (int i = 0; i < nmeas; i++){
			//System.out.println("modres= "+myLM.finalModelRes[i]);
			for (int j = 0; j < npars; j++){
				myLM.Jacobian.set(i, j, nnRes.jacobiMatrix[i][j + 3]);
			}
		}
	}
}
