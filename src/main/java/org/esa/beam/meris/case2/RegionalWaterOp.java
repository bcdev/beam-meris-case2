package org.esa.beam.meris.case2;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.meris.case2.fit.ChiSquareFitting;
import org.esa.beam.meris.case2.fit.MerisGLM;
import org.esa.beam.meris.case2.water.RegionalWater;
import org.esa.beam.meris.case2.water.WaterAlgorithm;

@OperatorMetadata(alias = "Meris.RegionalWater",
                  description = "Performs IOP retrieval on atmospherically corrected MERIS products.",
                  authors = "Roland Doerffer (GKSS); Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.5",
                  internal = true)
public class RegionalWaterOp extends MerisCase2BasisWaterOp {

    private static final String PRODUCT_TYPE_SUFFIX = "REG";

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

    @Override
    protected ChiSquareFitting createChiSquareFitting() {
        return new ChiSquareFitting(tsmConversionExponent, tsmConversionFactor,
                                    chlConversionExponent, chlConversionFactor, new MerisGLM(11, 8));
    }

    @Override
    protected String getProductTypeSuffix() {
        return PRODUCT_TYPE_SUFFIX;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(RegionalWaterOp.class);
        }
    }


}
