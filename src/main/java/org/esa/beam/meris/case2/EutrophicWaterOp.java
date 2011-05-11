package org.esa.beam.meris.case2;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.meris.case2.fit.ChiSquareFitting;
import org.esa.beam.meris.case2.fit.MerisGLM;
import org.esa.beam.meris.case2.water.EutrophicWater;
import org.esa.beam.meris.case2.water.WaterAlgorithm;

@OperatorMetadata(alias = "Meris.EutrophicWater",
                  description = "Performs IOP retrieval on atmospherically corrected MERIS products.",
                  authors = "Roland Doerffer (GKSS); Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.5",
                  internal = true)
public class EutrophicWaterOp extends MerisCase2BasisWaterOp {

    private static final String BAND_NAME_A_BTSM = "a_btsm";
    private static final String PRODUCT_TYPE_SUFFIX = "EUT";

    @SourceProduct(alias = "acProduct", label = "Atmospherically corrected product")
    private Product source;

    @Parameter(defaultValue = "1.0", description = "Exponent for conversion from TSM to B_TSM")
    private double tsmConversionExponent;
    @Parameter(defaultValue = "1.73", description = "Factor for conversion from TSM to B_TSM")
    private double tsmConversionFactor;
    @Parameter(defaultValue = "1.0", description = "Exponent for conversion from A_PIG to CHL_CONC")
    private double chlConversionExponent;
    @Parameter(defaultValue = "0.0318", description = "Factor for conversion from A_PIG to CHL_CONC")
    private double chlConversionFactor;


    @Override
    protected void addTargetBands(ProductConfigurer productConfigurer) {
        super.addTargetBands(productConfigurer);
        addTargetBand(productConfigurer, BAND_NAME_A_BTSM, "m^-1", "btsm absorption at 442 nm", true,
                      ProductData.TYPE_FLOAT32);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {
        super.configureTargetSamples(configurator);
        configurator.defineSample(EutrophicWater.TARGET_A_BTSM_INDEX, BAND_NAME_A_BTSM);
    }

    @Override
    protected String getDefaultForwardWaterNetResourcePath() {
        return "/org/esa/beam/meris/case2/eutrophic/30x15_88.8forw.net";
    }

    @Override
    protected String getDefaultInverseWaterNetResourcePath() {
        return "/org/esa/beam/meris/case2/eutrophic/60x20_586.8inv.net";
    }

    @Override
    protected WaterAlgorithm createAlgorithm() {
        return new EutrophicWater(getSpectrumOutOfScopeThreshold(),
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
            super(EutrophicWaterOp.class);
        }
    }


}
