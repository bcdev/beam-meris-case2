package org.esa.beam.case2.algorithm.fit;

/**
 * @author Schiller
 *
 */
public interface ModelInterf4LM {
	
	public void initSetOfFits(Object something, double waterReflLogVariance);
	public void initSingleFit(Object something);
	public void modelAndJacobian(double[] pars); /*model function & transformed CovMeas & gradient*/
	

}
