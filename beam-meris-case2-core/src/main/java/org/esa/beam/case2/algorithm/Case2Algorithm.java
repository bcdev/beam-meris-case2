package org.esa.beam.case2.algorithm;

import org.esa.beam.case2.algorithm.fit.MerisC2R_GLM;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.processor.ProcessorException;

public abstract class Case2Algorithm {

    public abstract OutputBands init(Product inputProduct, String[] inputBandNames,
                                     AlgorithmParameter parameter, Auxdata auxdata);

    public abstract void perform(PixelData pixel, OutputBands outputBands) throws
                                                                           ProcessorException /* end of retrieval and hope for success */;

    protected double correctViewAngle(double teta_view_deg, int pixelX, int centerPixel, boolean isFullResolution) {
        final double ang_coef_1 = -0.004793;
        final double ang_coef_2 = isFullResolution ? 0.0093247 / 4 : 0.0093247;
        teta_view_deg = teta_view_deg + Math.abs(pixelX - centerPixel) * ang_coef_2 + ang_coef_1;
        return teta_view_deg;
    }

    protected MerisC2R_GLM createGLM() {
        return new MerisC2R_GLM(11, 8);
    }
}
