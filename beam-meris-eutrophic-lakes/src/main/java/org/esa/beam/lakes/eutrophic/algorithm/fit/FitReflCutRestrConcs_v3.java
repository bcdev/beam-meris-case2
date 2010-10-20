package org.esa.beam.lakes.eutrophic.algorithm.fit;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.fit.ErrorFit3_v2;
import org.esa.beam.case2.util.nn.ForwNNReflCut;
import org.esa.beam.case2.util.nn.InvNNReflCut;
import org.esa.beam.case2.util.nn.NNCalc;

import java.io.IOException;
/*
 * Created on Jul 9, 2004
 *
 */

/**
 * @author schiller
 */
public class FitReflCutRestrConcs_v3 implements ErrorFit3_v2 {

    private AlgorithmParameter parameter;
    private double ReflCut;
    // the NN stuff
    private static InvNNReflCut invNN;
    private static ForwNNReflCut forwNN;

    // the outcomes of the fit
    private static double spm, cl, yel, chisq, chisqorig, paramChange;
    private static double[] nn2out, nn2outorig;
    private static int niter;

    //the stuff for chi**2-calculation

    private static double[] nn1in;
    private static double nn2in[];

    private static double[] posFit = new double[3];

    public FitReflCutRestrConcs_v3(double ReflCut, AlgorithmParameter parameter, double errscale) throws
                                                                                                 IOException {
        this.ReflCut = ReflCut;
        this.parameter = parameter;
        invNN = new InvNNReflCut(parameter.waterNnInverseFilePath, this.ReflCut);
        forwNN = new ForwNNReflCut(parameter.waterNnForwardFilePath, this.ReflCut);
        final double[] outmin = invNN.getOutmin();
        final double[] outmax = invNN.getOutmax();
        for (int i = 0; i < 3; i++) {
            LvMqRestrFit3_v3.range[0][i] = outmin[i];
            LvMqRestrFit3_v3.range[1][i] = outmax[i];
        }

        LvMqRestrFit3_v3.theCase = this;

        LvMqRestrFit3_v3.jacobi = new double[8][3];
        LvMqRestrFit3_v3.residuals = new double[8];
        LvMqRestrFit3_v3.posmin = new double[3];
        LvMqRestrFit3_v3.jactrjac = new double[3][3];
        LvMqRestrFit3_v3.grad = new double[3];

        nn1in = new double[11];
        nn2in = new double[6];
        nn2out = new double[8];

    }

    //Konstruktor nur fuer hiesige main-Methode
    public FitReflCutRestrConcs_v3(double ReflCut, String invNet, String forwNet, double errscale) throws
                                                                                                   IOException {
        this.ReflCut = ReflCut;
        invNN = new InvNNReflCut(invNet, this.ReflCut);
        forwNN = new ForwNNReflCut(forwNet, this.ReflCut);
        final double[] inmin = forwNN.getInmin();
        final double[] inmax = forwNN.getInmax();
        for (int i = 0; i < 3; i++) {
            LvMqRestrFit3_v3.range[0][i] = inmin[3 + i];
            LvMqRestrFit3_v3.range[1][i] = inmax[3 + i];
        }

        LvMqRestrFit3_v3.theCase = this;
        LvMqRestrFit3_v3.jacobi = new double[8][3];
        LvMqRestrFit3_v3.residuals = new double[8];
        LvMqRestrFit3_v3.posmin = new double[3];
        LvMqRestrFit3_v3.jactrjac = new double[3][3];
        LvMqRestrFit3_v3.grad = new double[3];

        nn1in = new double[11];
        nn2in = new double[6];
        nn2out = new double[8];

    }

    @Override
    public void processPixelMod(double[] inFit) {
        double[] refl = new double[8];
        for (int i = 0; i < 8; i++) {
            refl[i] = inFit[i + 3];
        }
        this.processPixel(refl, inFit[0], inFit[2], inFit[1], 0.0);
    }

    private void processPixel(double[] refl, double sz, double sa, double vz, double va) {
        nn1in[0] = sz;
        nn1in[1] = vz;
        double ad = sa - va;
        if (ad > 180.0) {
            ad = 360.0 - ad;
        }
        nn1in[2] = ad;
        int count = 0;
        for (int i = 0; i < refl.length; i++) {
            nn1in[3 + i] = Math.log(refl[i] / Math.PI);
        }
        //LvMqRestrFit3_v2COG.start=invNN.calc(nn1in);
        nn2in[0] = nn1in[0];
        nn2in[1] = nn1in[1];
        nn2in[2] = nn1in[2];
        for (int i = 0; i < 3; i++) {
            nn2in[i + 3] = LvMqRestrFit3_v3.start[i];
        }
        LvMqRestrFit3_v3.niter = 0;
        nn2outorig = forwNN.calc(nn2in);// fuer den Fit nicht noetig - wird gemacht fuer den Vergleich
        if (invNN.count < 7) {
            LvMqRestrFit3_v3.go_v3(parameter.nu, parameter.tau, parameter.eps1, parameter.eps2, parameter.nIterMax);
        }
        chisq = LvMqRestrFit3_v3.funcmin;
        chisqorig = LvMqRestrFit3_v3.funcstart;
        niter = LvMqRestrFit3_v3.niter;
        for (int i = 0; i < 3; i++) {
            nn2in[i + 3] = LvMqRestrFit3_v3.posmin[i];
        }
        nn2out = forwNN.calc(nn2in);// fuer den Fit nicht noetig - wird gemacht fuer den Vergleich
//		for(int i=0;i<8;i++) {
//			System.out.println("refls: " + nn1in[i+3] + "   " + nn2outorig[i]+ "   " + nn2out[i]);
//		}
        //System.out.println("niter=" +niter+" chiorig="+ chisqorig+" chisq="+chisq);

        System.arraycopy(LvMqRestrFit3_v3.posmin, 0, posFit, 0, posFit.length);
        opt2conc();
    }

    @Override
    public void theError(double[] concs) {
        for (int i = 0; i < 3; i++) {
            nn2in[i + 3] = concs[i];
        }

        //System.out.println(invNN.netname);

        NNCalc forwres = forwNN.calcJacobi(nn2in);
        double sum = 0.0;
        final double[][] jacobiMatrix = forwres.getJacobiMatrix();
        final double[] nnOutput = forwres.getNnOutput();
        for (int i = 0; i < 8; i++) {
            System.arraycopy(jacobiMatrix[i], 3, LvMqRestrFit3_v3.jacobi[i], 0, 3);
            LvMqRestrFit3_v3.residuals[i] = nnOutput[i] - nn1in[i + 3];
            sum += LvMqRestrFit3_v3.residuals[i] * LvMqRestrFit3_v3.residuals[i];
        }
        //System.out.println("sum "+sum);
        LvMqRestrFit3_v3.errorsquared = sum / 2.0;
    }

    @Override
    public void jactrjac_grad() {
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                double sum = 0;
                for (int l = 0; l < 8; l++) {
                    sum += LvMqRestrFit3_v3.jacobi[l][i] * LvMqRestrFit3_v3.jacobi[l][k];
                }
                LvMqRestrFit3_v3.jactrjac[i][k] = sum;
            }
        }

        for (int i = 0; i < 3; i++) {
            LvMqRestrFit3_v3.grad[i] = 0.0;
            for (int l = 0; l < 8; l++) {
                LvMqRestrFit3_v3.grad[i] += LvMqRestrFit3_v3.jacobi[l][i] * LvMqRestrFit3_v3.residuals[l];
            }
        }
    }

    private void opt2conc() {
        spm = 1.73 * Math.exp(LvMqRestrFit3_v3.posmin[0]);
        cl = 24.0 * Math.exp(LvMqRestrFit3_v3.posmin[1]);
        yel = Math.exp(LvMqRestrFit3_v3.posmin[2]);
    }

}
