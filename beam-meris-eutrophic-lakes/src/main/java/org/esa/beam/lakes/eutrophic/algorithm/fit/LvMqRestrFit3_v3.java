package org.esa.beam.lakes.eutrophic.algorithm.fit;


/**
 * @author Schiller
 *
 */
final class LvMqRestrFit3_v3 {

	// die Parameter des Fits


	//zum Start des Fits:
	public static double[] start = new double[3];

	// der zulaessige Fit-Bereich
	public static double[][] range = new double[2][3];

	// die Ergebnisse des Fits:
	public static int niter;

	public static double[] posmin = new double[3];

	public static double funcmin;

	public static double funcstart;

	// die Fehlerfunktion und ihre Ausgabewerte, werden gesetzt durch theCase.theError(fitParameter)
	public static FitReflCutRestrConcs_v3 theCase;

	public static double errorsquared;

	public static double[] residuals; // [#Terme in Fehlersumme]

	public static double[][] jacobi;  // [#Terme in Fehlersumme][#zu fittende Parameter]
	
	public static double[][] jactrjac;  // jacobi'*jacobi
	
	public static double[] grad;  // gradient=jacobi'*residuals
	
	/*
	 * K. Madsen, H. B. Nielsen, O. Tingleff
	 * Methods for non-linear Least Squares Problems
	 * p. 27
	 *  www2.imm.dtu.dk/pubdb/views/edoc_download.php/3215/pdf/imm3215.pdf
	 */
	public static void go_v3(double nu, final double tau, final double eps1, final double eps2, final int nitermax) {
		double newposmin[];
        double invarg[][];
        double invmat[][];
        double mu;
        double newfuncmin;
        newposmin = new double[3];
		invarg = new double[3][3];
		//System.out.println("fit.go------------------------------------------------------------------");
		for (int i = 0; i < 3; i++)
			posmin[i] = 0.5*(range[0][i]+range[1][i]);
		theCase.theError(posmin);
		theCase.jactrjac_grad();
		//System.out.println(errorsquared);
		//System.out.println(posmin[0]+"  "+posmin[1]+"  "+posmin[2]+"       "+ errorsquared);
		funcstart = errorsquared;
		funcmin = funcstart;
        niter = 0; //nu=2; tau=5.e-2; eps1=1.e-2; eps2=3.e-4; nitermax=30;
		double maxaii=0;
		for (int i=0; i<3; i++) if(jactrjac[i][i]>maxaii) maxaii=jactrjac[i][i];
		mu=maxaii*tau;
		//System.out.println("mu= "+ mu);
		boolean found=mincnorm(grad, 3)< eps1;
		while ( !found  && (niter<nitermax) ) {
			niter++;
			//System.out.println("niter="+niter+"  funcmin="+funcmin);
			for (int i = 0; i < 3; i++) 	jactrjac[i][i] += mu;
			invmat = invmat33(jactrjac);
			double[] hlm=new double[3];
			for (int i = 0; i < 3; i++) {
				double sum = 0.;
					for (int j = 0; j < 3; j++)
						sum += invmat[i][j] * grad[j];
				hlm[i] = -sum;
			}
			//System.out.println("steplgth="+euclnorm(hlm, 3));
			if ( euclnorm(hlm, 3) <= eps2*(euclnorm(posmin, 3) + eps2 ) )
				found=true;
			else {
				for (int i=0; i<3; i++) newposmin[i] = posmin[i] + hlm[i];
				theCase.theError(newposmin);
				//System.out.println(errorsquared);
				double[] hdiff=new double[3];
				for (int i=0; i<3; i++) hdiff[i]=mu*hlm[i]-grad[i];
				double rho=(funcmin-errorsquared)/(0.5*scalpro(hlm, hdiff, 3));
				if (rho > 0) {
					theCase.jactrjac_grad();
					funcmin=errorsquared;
					for (int i=0; i<3; i++) posmin[i] = newposmin[i];
					//System.out.println("niter="+niter+"  |gr|="+mincnorm(grad, 3)+"  fm="+funcmin);
					found = ( mincnorm(grad, 3) <= eps1);
					double mx=0.33333;
					double dh=(2.*rho-1);
					dh=1. - dh*dh*dh;
					if (dh > mx) mx=dh;
					mu=mu*mx;
					//System.out.println("1mu= "+ mu);
					nu=2.;
				} else {
					mu=mu*nu;
					nu=3.*nu;
					//System.out.println("2mu= "+ mu);
				}
			}
		}
		int iu=1, io=8;
		boolean offRange=false;
		for (int i = 0; i < 3; i++) {
			if (posmin[i] < range[0][i]) {
				newposmin[i] = range[0][i];
				offRange=true;
				//System.out.println(i+" unten "+posmin[i]+" now "+newposmin[i]);
			}
			if (posmin[i] > range[1][i]) {
				newposmin[i] = range[1][i];
				offRange=true;
				//System.out.println(i+" oben "+posmin[i]+" now "+newposmin[i]);
			}
			iu*=2; io*=2;
		}
		if (offRange) {
			//System.out.println("offRange---------------------------------------------!");
			//System.out.println("funcmin from fit "+funcmin+" at");
			//for (int i=0; i<3; i++) System.out.println(posmin[i]+" now "+newposmin[i]);
			for (int i=0; i<3; i++) posmin[i]=newposmin[i];
			theCase.theError(posmin);
			theCase.jactrjac_grad();
			//System.out.println("funcmin now "+funcmin);
		}
		//System.out.println(posmin[0]+"  "+posmin[1]+"  "+posmin[2]+"       "+ errorsquared+"  "+niter);
		//System.out.println("-----------------------------------------------------------  ");
	}
	
	static double scalpro(double[] x, double[] y, int dim) {
		double res=0.;
		for (int i=0; i<dim; i++) res=res+x[i]*y[i];
		return res;
	}
	
	static double euclnorm(double[] x, int dim) {
		return Math.sqrt(scalpro(x, x, dim));
	}
	
	static double mincnorm(double[] x, int dim) {
		double res=0.;
		for (int i=0; i<dim; i++) {
			double axc=Math.abs(x[i]);
			if (res<axc) res=axc;
		}
		return res;
	}
	public static double[][] invmat33(double[][] a) {
		/* in der Schnelle kein Matrixpaket gefunden */

		double invdet = 1. / (a[0][0] * a[1][1] * a[2][2] - a[0][0] * a[1][2]
				* a[2][1] - a[1][0] * a[0][1] * a[2][2] + a[1][0] * a[0][2]
				* a[2][1] + a[2][0] * a[0][1] * a[1][2] - a[2][0] * a[0][2]
				* a[1][1]);

		double[][] b = new double[3][3];
		b[0][0] = (a[1][1] * a[2][2] - a[1][2] * a[2][1]) * invdet;
		b[0][1] = -(a[0][1] * a[2][2] - a[0][2] * a[2][1]) * invdet;
		b[0][2] = (a[0][1] * a[1][2] - a[0][2] * a[1][1]) * invdet;
		b[1][0] = -(a[1][0] * a[2][2] - a[1][2] * a[2][0]) * invdet;
		b[1][1] = (a[0][0] * a[2][2] - a[0][2] * a[2][0]) * invdet;
		b[1][2] = -(a[0][0] * a[1][2] - a[0][2] * a[1][0]) * invdet;
		b[2][0] = (a[1][0] * a[2][1] - a[1][1] * a[2][0]) * invdet;
		b[2][1] = -(a[0][0] * a[2][1] - a[0][1] * a[2][0]) * invdet;
		b[2][2] = (a[0][0] * a[1][1] - a[0][1] * a[1][0]) * invdet;

		return b;
	}

	public static void main(String[] args) {
		double[][] b = new double[3][3];
		b[0][0] = 1;
		b[0][1] = 2;
		b[0][2] = 3;
		b[1][0] = 4;
		b[1][1] = 5;
		b[1][2] = 6;
		b[2][0] = 7;
		b[2][1] = 8;
		b[2][2] = 10;
		double[][] c = invmat33(b);
		System.out.println(b[0][0] + "   " + b[0][1] + "   " + b[0][2]);
		System.out.println(b[1][0] + "   " + b[1][1] + "   " + b[1][2]);
		System.out.println(b[2][0] + "   " + b[2][1] + "   " + b[2][2]);
		System.out.println();
		System.out.println(c[0][0] + "   " + c[0][1] + "   " + c[0][2]);
		System.out.println(c[1][0] + "   " + c[1][1] + "   " + c[1][2]);
		System.out.println(c[2][0] + "   " + c[2][1] + "   " + c[2][2]);
	}
}