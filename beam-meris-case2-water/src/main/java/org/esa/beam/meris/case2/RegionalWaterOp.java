package org.esa.beam.meris.case2;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.meris.case2.algorithm.RegionalWater;
import org.esa.beam.meris.case2.algorithm.WaterAlgorithm;

@OperatorMetadata(alias = "Meris.RegionalWater",
                  description = "Performs IOP retrieval on atmospherically corrected MERIS products.",
                  authors = "Roland Doerffer (GKSS); Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0")
public class RegionalWaterOp extends MerisCase2BasisWaterOp {

    @SourceProduct(alias = "acProduct", label = "Atmospherically corrected product")
    private Product source;

    @Parameter(defaultValue = "1.0", description = "Exponent for conversion from TSM to B_TSM")
    private double tsmConversionExponent;
    @Parameter(defaultValue = "1.73", description = "Factor for conversion from TSM to B_TSM")
    private double tsmConversionFactor;
    @Parameter(defaultValue = "1.04", description = "Exponent for conversion from A_PIG to CHL_CONC")
    private double chlConversionExponent;
    @Parameter(defaultValue = "21.0", description = "Factor for conversion from A_PIG to CHL_CONC")
    private double chlConversionFactor;


    @Override
    protected String getDefaultForwardWaterNetResourcePath() {
        return "/org/esa/beam/meris/case2/regional/meris_fn_20040319_15x15x15_1750.4.net";
    }

    @Override
    protected String getDefaultInverseWaterNetResourcePath() {
        return "/org/esa/beam/meris/case2/regional/meris_bn_20040322_45x16x12x8x5_5177.9.net";
    }

    @Override
    protected WaterAlgorithm createAlgorithm() {
        return new RegionalWater(getSpectrumOutOfScopeThreshold(),
                                 tsmConversionExponent, tsmConversionFactor,
                                 chlConversionExponent, chlConversionFactor);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(RegionalWaterOp.class);
        }
    }


}
