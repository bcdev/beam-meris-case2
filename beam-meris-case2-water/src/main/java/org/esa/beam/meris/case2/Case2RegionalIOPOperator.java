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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.io.File;


@OperatorMetadata(alias = "Meris.Case2RegionalIOP",
                  description = "Performs IOP retrieval on L1b MERIS products, including radiometric correction and atmospheric correction.",
                  authors = "Roland Doerffer (GKSS); Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0",
                  internal = true)
public class Case2RegionalIOPOperator extends Operator {

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    ///////////  GlintCorrectionOperator  ///////////////////////////
    ///////////

    @Parameter(defaultValue = "true", label = "Perform atmospheric correction",
               description = "Whether or not to perform atmospheric correction.")
    private boolean doAtmosphericCorrection;

    @Parameter(defaultValue = "true",
               label = "Perform SMILE correction",
               description = "Whether to perform SMILE correction.")
    private boolean doSmileCorrection;

    @Parameter(defaultValue = "true", label = "Output TOSA reflectance",
               description = "Toggles the output of TOSA reflectance.")
    private boolean outputTosa;

    @Parameter(defaultValue = "true", label = "Output path reflectance",
               description = "Toggles the output of water leaving path reflectance.")
    private boolean outputPath;

    @Parameter(defaultValue = "true", label = "Output transmittance",
               description = "Toggles the output of downwelling irrediance transmittance.")
    private boolean outputTransmittance;

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

    ///////////  RegionalWaterOp  ///////////////////////////
    ///////////
    @Parameter(defaultValue = "1.0", description = "Exponent for conversion from TSM to B_TSM")
    private double tsmConversionExponent;

    @Parameter(defaultValue = "1.73", description = "Factor for conversion from TSM to B_TSM")
    private double tsmConversionFactor;

    @Parameter(defaultValue = "1.04", description = "Exponent for conversion from A_PIG to CHL_CONC")
    private double chlConversionExponent;

    @Parameter(defaultValue = "21.0", description = "Factor for conversion from A_PIG to CHL_CONC")
    private double chlConversionFactor;

    @Parameter(defaultValue = "4.0", description = "Threshold to indicate Spectrum is Out of Scope")
    private double spectrumOutOfScopeThreshold;

    @Parameter(defaultValue = "agc_flags.INVALID",
               description = "Expression defining pixels not considered for processing")
    private String invalidPixelExpression;

    @Parameter(label = "Inverse water neural net (optional)",
               description = "The file of the inverse water neural net to be used instead of the default.")
    private File inverseWaterNnFile;

    @Parameter(label = "Forward water neural net (optional)",
               description = "The file of the forward water neural net to be used instead of the default.")
    private File forwardWaterNnFile;

    @Parameter(label = "Perform Chi-Square fitting", defaultValue = "false",
               description = "Whether or not to perform the Chi-Square fitting.")
    private boolean performChiSquareFit;


    @Override
    public void initialize() throws OperatorException {
        Product inputProduct = sourceProduct;

        if (doAtmosphericCorrection) {
            Operator atmoCorOp = new GlintCorrectionOperator();
            atmoCorOp.setParameter("doSmileCorrection", doSmileCorrection);
            atmoCorOp.setParameter("outputReflec", true);
            atmoCorOp.setParameter("outputTosa", outputTosa);
            atmoCorOp.setParameter("outputPath", outputPath);
            atmoCorOp.setParameter("outputTransmittance", outputTransmittance);
            atmoCorOp.setParameter("landExpression", landExpression);
            atmoCorOp.setParameter("cloudIceExpression", cloudIceExpression);
            atmoCorOp.setSourceProduct("merisProduct", inputProduct);
            inputProduct = atmoCorOp.getTargetProduct();
        }

        Operator case2Op = new RegionalWaterOp();
        case2Op.setParameter("tsmConversionExponent", tsmConversionExponent);
        case2Op.setParameter("tsmConversionFactor", tsmConversionFactor);
        case2Op.setParameter("chlConversionExponent", chlConversionExponent);
        case2Op.setParameter("chlConversionFactor", chlConversionFactor);
        case2Op.setParameter("spectrumOutOfScopeThreshold", spectrumOutOfScopeThreshold);
        case2Op.setParameter("invalidPixelExpression", invalidPixelExpression);
        case2Op.setParameter("inverseWaterNnFile", inverseWaterNnFile);
        case2Op.setParameter("forwardWaterNnFile", forwardWaterNnFile);
        case2Op.setParameter("performChiSquareFit", performChiSquareFit);
        case2Op.setSourceProduct("acProduct", inputProduct);

        setTargetProduct(case2Op.getTargetProduct());
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Case2RegionalIOPOperator.class);
        }
    }

}
