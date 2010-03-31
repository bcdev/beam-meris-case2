package org.esa.beam.case2.algorithm;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.processor.ProcessorException;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public interface Case2Algorithm {


    OutputBands init(Product inputProduct, String[] inputBandNames,
                            AlgorithmParameter parameter, Auxdata auxdata);

    void perform(PixelData pixel, OutputBands outputBands) throws ProcessorException /* end of retrieval and hope for success */;
}
