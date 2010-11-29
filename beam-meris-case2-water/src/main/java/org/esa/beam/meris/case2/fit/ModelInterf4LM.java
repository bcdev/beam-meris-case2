package org.esa.beam.meris.case2.fit;

/**
 * @author Schiller
 */
public interface ModelInterf4LM {

    void initSetOfFits(Object something, double waterReflLogVariance);

    void initSingleFit(Object something);

    void modelAndJacobian(double[] pars); /*model function & transformed CovMeas & gradient*/


}
