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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.meris.case2.fit.ChiSquareFitting;
import org.esa.beam.meris.case2.fit.MerisGLM;
import org.esa.beam.meris.case2.water.BorealWater;
import org.esa.beam.meris.case2.water.WaterAlgorithm;

@OperatorMetadata(alias = "Meris.BorealWater",
                  description = "Performs IOP retrieval on atmospherically corrected MERIS products.",
                  authors = "Roland Doerffer (GKSS); Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.5.7",
                  internal = true)
public class BorealWaterOp extends MerisCase2BasisWaterOp {

    private static final String PRODUCT_TYPE_SUFFIX = "BOR";

    @SourceProduct(alias = "acProduct", label = "Atmospherically corrected product")
    private Product source;

    @Override
    protected String getDefaultForwardWaterNetResourcePath() {
        return "/org/esa/beam/meris/case2/boreal/15x15x15_96.5.net";
    }

    @Override
    protected String getDefaultInverseWaterNetResourcePath() {
        return "/org/esa/beam/meris/case2/boreal/45x16x12x8_44.8.net";
    }

    @Override
    protected WaterAlgorithm createAlgorithm() {
        return new BorealWater(getSpectrumOutOfScopeThreshold());
    }

    @Override
    protected ChiSquareFitting createChiSquareFitting() {
        return new ChiSquareFitting(1.0, 0.0, 1.0, 0.0, new MerisGLM(10, 7));

    }

    @Override
    protected String getProductTypeSuffix() {
        return PRODUCT_TYPE_SUFFIX;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BorealWaterOp.class);
        }
    }


}
