package org.esa.beam.case2.algorithm.fit;

import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public interface ChiSquareFit {

    void init(double tsmConversionExponent, double tsmConversionFactor, double chlConversionExponent,
              double chlConversionFactor, NNffbpAlphaTabFast forwardWaterNet, MerisGLM glm
    );

    void perform(double teta_sun_deg, double teta_view_deg, double azi_diff_deg, double[] RLw_cut,
                 OutputBands outputBands);
}
