package org.esa.beam.case2.algorithm.fit;
/*
 * Created on Jul 5, 2004
 *
 */

/**
 * @author schiller
 *
 * 
 * die vars sind die zu varieirenden Parameter, die '3' erinnert daran,
 * dass noch alles fest dimensioniert ist fuer 3 Konzentrationen, 8 Reflektanzen etc
 */


public interface ErrorFit3_v2 {

    public void theError(double[] vars);
    
    public void jactrjac_grad();

    void processPixelMod(double[] inFit);

}

