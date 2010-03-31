package org.esa.beam.case2.algorithm.fit;

import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.Auxdata;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public interface ChiSquareFit {

    void init(AlgorithmParameter parameter, Auxdata auxdata);

    void perform(double teta_sun_deg, double teta_view_deg, double azi_diff_deg, double[] RLw_cut,
                 OutputBands outputBands);
}
