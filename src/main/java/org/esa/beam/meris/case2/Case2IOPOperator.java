/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.meris.case2;

import org.esa.beam.atmosphere.operator.GlintCorrectionOperator;
import org.esa.beam.atmosphere.operator.ReflectanceEnum;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import java.io.File;


@OperatorMetadata(alias = "Meris.Case2Regional",
                  description = "Performs IOP retrieval on L1b MERIS products, including radiometric correction and atmospheric correction.",
                  authors = "Roland Doerffer (GKSS); Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2011 by Brockmann Consult",
                  version = "1.7-CC")
public class Case2IOPOperator extends Operator {

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    ///////////  GlintCorrectionOperator  ///////////////////////////
    ///////////

    @Parameter(defaultValue = "true", label = "Perform atmospheric correction",
               description = "Whether or not to perform atmospheric correction.")
    private boolean doAtmosphericCorrection;

    @Parameter(label = "Alternative atm. corr. neural net (optional)",
               defaultValue = GlintCorrectionOperator.MERIS_ATMOSPHERIC_NET_NAME,
               description = "The file of the atmospheric net to be used instead of the default neural net.")
    private File atmoNetFile;

    @Parameter(defaultValue = "true",
               label = "Perform SMILE correction",
               description = "Whether to perform SMILE correction.")
    private boolean doSmileCorrection;

    @Parameter(defaultValue = "false", label = "Output TOSA reflectance",
               description = "Toggles the output of TOSA reflectance.")
    private boolean outputTosa;

    @Parameter(defaultValue = "true", label = "Output water leaving reflectance",
               description = "Toggles the output of water leaving reflectance.")
    private boolean outputReflec;

    @Parameter(defaultValue = "RADIANCE_REFLECTANCES", valueSet = {"RADIANCE_REFLECTANCES", "IRRADIANCE_REFLECTANCES"},
               label = "Output water leaving reflectance as",
               description = "Select if reflectances shall be written as radiances or irradiances. " +
                       "The irradiances are compatible with standard MERIS product.")
    private ReflectanceEnum outputReflecAs;

    @Parameter(defaultValue = "true", label = "Output path reflectance",
               description = "Toggles the output of water leaving path reflectance.")
    private boolean outputPath;

    @Parameter(defaultValue = "false", label = "Output transmittance",
               description = "Toggles the output of downwelling irradiance transmittance.")
    private boolean outputTransmittance;

    @Parameter(defaultValue = "false", label = "Output Kd spectrum",
               description = "Toggles the output of downwelling irradiance attenuation coefficients. " +
                       "If disabled only Kd_490 is added to the output.")
    private boolean outputKdSpectrum;

    @Parameter(defaultValue = "false",
               label = "Output normalised bidirectional reflectances",
               description = "Toggles the output of normalised reflectances.")
    private boolean outputNormReflec;

    @Parameter(defaultValue = "false", label = "Output A_Poc",
               description = "Toggles the output of absorption by particulate organic matter.")
    private boolean outputAPoc;

    @Parameter(defaultValue = "toa_reflec_10 > toa_reflec_6 AND toa_reflec_13 > 0.0475",
               label = "Land detection expression",
               description = "The arithmetic expression used for land detection.",
               notEmpty = true, notNull = true)
    private String landExpression;

    @Parameter(defaultValue = "toa_reflec_14 > 0.2",
               label = "Cloud/Ice detection expression",
               description = "The arithmetic expression used for cloud/ice detection.",
               notEmpty = true, notNull = true)
    private String cloudIceExpression;

    ///////////  Case2WaterOp  ///////////////////////////
    ///////////

    @Parameter(label = "Use climatology map for salinity and temperature", defaultValue = "true",
               description = "By default a climatology map is used. If set to 'false' the specified average values are used " +
                       "for the whole scene.")
    private boolean useSnTMap;

    @Parameter(label = "Output salinity and temperature bands", defaultValue = "false",
               description = "Toggles the output of the salinity and temperature band.")
    private boolean outputSnT;

    @Parameter(label = "Average salinity", defaultValue = "35", unit = "PSU",
               description = "The salinity of the water.")
    private double averageSalinity;

    @Parameter(label = "Average temperature", defaultValue = "15", unit = "°C", description = "The water temperature.")
    private double averageTemperature;

    @Parameter(label = "Tsm conversion exponent",
               defaultValue = "1.0",
               description = "Exponent for conversion from TSM to B_TSM.")
    private Double tsmConversionExponent;

    @Parameter(label = "Tsm conversion factor",
               defaultValue = "1.73",
               description = "Factor for conversion from TSM to B_TSM.")
    private Double tsmConversionFactor;

    @Parameter(label = "Chl conversion factor",
               defaultValue = "1.04",
               description = "Exponent for conversion from A_PIG to CHL_CONC")
    private double chlConversionExponent;

    @Parameter(label = "Chl conversion exponent",
               defaultValue = "21.0",
               description = "Factor for conversion from A_PIG to CHL_CONC")
    private double chlConversionFactor;

    @Parameter(defaultValue = "4.0", description = "Threshold to indicate Spectrum is Out of Scope.")
    private double spectrumOutOfScopeThreshold;

    @Parameter(defaultValue = "agc_flags.INPUT_INVALID",
               description = "Expression defining pixels not considered for processing.")
    private String invalidPixelExpression;

    @Parameter(label = "Alternative inverse iop neural net (optional)",
               defaultValue = RegionalWaterOp.DEFAULT_INVERSE_IOP_NET,
               description = "The file of the inverse iop neural net to be used instead of the default.")
    private File inverseIopNnFile;

    @Parameter(label = "Alternative inverse kd neural net (optional)",
               defaultValue = RegionalWaterOp.DEFAULT_INVERSE_KD_NET,
               description = "The file of the inverse kd neural net to be used instead of the default.")
    private File inverseKdNnFile;

    @Parameter(label = "Alternative forward iop neural net (optional)",
               defaultValue = RegionalWaterOp.DEFAULT_FORWARD_IOP_NET,
               description = "The file of the forward iop neural net to be used instead of the default.")
    private File forwardIopNnFile;

    @Override
    public void initialize() throws OperatorException {
        Product inputProduct = sourceProduct;

        if (doAtmosphericCorrection) {
            Operator atmoCorOp = new GlintCorrectionOperator();
            atmoCorOp.setParameter("doSmileCorrection", doSmileCorrection);
            if (atmoNetFile != null) {
                atmoCorOp.setParameter("atmoNetMerisFile", atmoNetFile);
            }
            atmoCorOp.setParameter("outputReflec", true);
            atmoCorOp.setParameter("outputReflecAs", outputReflecAs);
            atmoCorOp.setParameter("outputTosa", outputTosa);
            atmoCorOp.setParameter("outputNormReflec", outputNormReflec);
            atmoCorOp.setParameter("outputPath", outputPath);
            atmoCorOp.setParameter("outputTransmittance", outputTransmittance);
            atmoCorOp.setParameter("useSnTMap", useSnTMap);
            atmoCorOp.setParameter("averageSalinity", averageSalinity);
            atmoCorOp.setParameter("averageTemperature", averageTemperature);
            atmoCorOp.setParameter("landExpression", landExpression);
            atmoCorOp.setParameter("cloudIceExpression", cloudIceExpression);
            atmoCorOp.setSourceProduct("merisProduct", inputProduct);
            inputProduct = atmoCorOp.getTargetProduct();
        }

        Operator case2Op = new RegionalWaterOp();

        case2Op.setParameter("useSnTMap", useSnTMap);
        case2Op.setParameter("outputSnT", outputSnT);
        case2Op.setParameter("averageSalinity", averageSalinity);
        case2Op.setParameter("averageTemperature", averageTemperature);
        case2Op.setParameter("inputReflecAre", outputReflecAs);
        case2Op.setParameter("spectrumOutOfScopeThreshold", spectrumOutOfScopeThreshold);
        case2Op.setParameter("invalidPixelExpression", invalidPixelExpression);
        case2Op.setParameter("inverseIopNnFile", inverseIopNnFile);
        case2Op.setParameter("inverseKdNnFile", inverseKdNnFile);
        case2Op.setParameter("forwardIopNnFile", forwardIopNnFile);
        case2Op.setParameter("outputKdSpectrum", outputKdSpectrum);
        case2Op.setParameter("outputAPoc", outputAPoc);
        case2Op.setParameter("tsmConversionExponent", tsmConversionExponent);
        case2Op.setParameter("tsmConversionFactor", tsmConversionFactor);
        case2Op.setSourceProduct("acProduct", inputProduct);
        final Product case2Product = case2Op.getTargetProduct();

        Product targetProduct = new Product(case2Product.getName(), case2Product.getProductType(),
                                            case2Product.getSceneRasterWidth(), case2Product.getSceneRasterHeight());
        ProductUtils.copyTiePointGrids(inputProduct, targetProduct);
        ProductUtils.copyGeoCoding(inputProduct, targetProduct);
        targetProduct.setStartTime(inputProduct.getStartTime());
        targetProduct.setEndTime(inputProduct.getEndTime());

        final String[] names = inputProduct.getBandNames();
        for (String name : names) {
            if (name.contains("flags") || name.contains("b_tsm") || name.contains("a_tot")) {
                continue;
            }
            if (!outputReflec && name.startsWith("reflec")) {
                continue;
            }
            if (case2Product.containsBand(name)) {
                continue;
            }
            ProductUtils.copyBand(name, inputProduct, targetProduct, true);
        }

        final String[] case2names = case2Product.getBandNames();

        for (String name : case2names) {
            if (inputProduct.getGeoCoding() instanceof PixelGeoCoding &&
                    (name.startsWith("corr_") || name.startsWith("l1_flags"))) {
                continue;
            }
            if (!targetProduct.containsBand(name)) {
                ProductUtils.copyBand(name, case2Product, targetProduct, true);
            }
        }

        setTargetProduct(targetProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Case2IOPOperator.class);
        }
    }

}
