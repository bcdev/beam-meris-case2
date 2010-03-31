package org.esa.beam.lakes.eutrophic.algorithm.fit;
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

import Jama.*;

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
