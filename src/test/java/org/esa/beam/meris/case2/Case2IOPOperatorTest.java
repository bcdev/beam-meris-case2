package org.esa.beam.meris.case2;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Color;
import java.text.ParseException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class Case2IOPOperatorTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    @Test
    public void testInitializationWithAtmoCorr() throws Exception {
        final Product c2rProduct = GPF.createProduct("Meris.Case2Regional", GPF.NO_PARAMS, getL1bProduct());
        final String[] bandNames = c2rProduct.getBandNames();
        final String[] expectedTargetBands = {
                "reflec_1", "reflec_2", "reflec_3",
                "reflec_4", "reflec_5", "reflec_6",
                "reflec_7", "reflec_8", "reflec_9",
                "reflec_10", "reflec_12", "reflec_13",
                "path_1", "path_2", "path_3",
                "path_4", "path_5", "path_6",
                "path_7", "path_8", "path_9",
                "path_10", "path_12", "path_13",
                "tau_550", "tau_778", "tau_865",
                "glint_ratio",
                "ang_443_865",
                "detector_index",
                "a_ys_443",
                "a_pig_443",
                "a_total_443",
                "bb_spm_443",
                "tsm",
                "chl_conc",
                "chiSquare",
                "K_min",
                "Z90_max",
                "Kd_490",
                "turbidity_index",
                "agc_flags",
                "l1_flags",
                "case2_flags"
        };
        Assert.assertArrayEquals(expectedTargetBands, bandNames);
        ProductNodeGroup<Mask> maskGroup = c2rProduct.getMaskGroup();
        assertTrue(maskGroup.getNodeCount() > 0);
        assertTrue(maskGroup.contains("agc_land"));
        assertTrue(maskGroup.contains("cloud_ice"));
        assertTrue(maskGroup.contains("solzen"));
        assertTrue(maskGroup.contains("agc_invalid"));
        assertTrue(maskGroup.contains("case2_wlr_oor"));
        assertTrue(maskGroup.contains("case2_whitecaps"));
        assertTrue(maskGroup.contains("case2_invalid"));

    }

    @Test
    public void testInitializationWithoutAtmoCorr() throws Exception {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("doAtmosphericCorrection", false);
        parameters.put("invalidPixelExpression", "ac_flags.INVALID");
        final Product c2rProduct = GPF.createProduct("Meris.Case2Regional", parameters, getAtmoCorrectedProduct());
        final String[] bandNames = c2rProduct.getBandNames();
        final String[] expectedTargetBands = {
                "reflec_1", "reflec_2", "reflec_3",
                "reflec_4", "reflec_5", "reflec_6",
                "reflec_7", "reflec_8", "reflec_9",
                "reflec_10", "reflec_12", "reflec_13",
                "a_ys_443",
                "a_pig_443",
                "a_total_443",
                "bb_spm_443",
                "tsm",
                "chl_conc",
                "chiSquare",
                "K_min",
                "Z90_max",
                "Kd_490",
                "turbidity_index",
                "ac_flags",
                "case2_flags"
        };
        assertArrayEquals(expectedTargetBands, bandNames);
        ProductNodeGroup<Mask> maskGroup = c2rProduct.getMaskGroup();
        assertTrue(maskGroup.getNodeCount() > 0);
        assertTrue(maskGroup.contains("AC_INVALID"));
        assertTrue(maskGroup.contains("case2_wlr_oor"));
        assertTrue(maskGroup.contains("case2_whitecaps"));
        assertTrue(maskGroup.contains("case2_invalid"));


    }

    @Test
    public void testOuputReflectances() throws Exception {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("doAtmosphericCorrection", false);
        parameters.put("outputReflec", false);
        parameters.put("invalidPixelExpression", "ac_flags.INVALID");
        final Product c2rProduct = GPF.createProduct("Meris.Case2Regional", parameters, getAtmoCorrectedProduct());
        final String[] bandNames = c2rProduct.getBandNames();
        final String[] expectedTargetBands = {
                "a_ys_443",
                "a_pig_443",
                "a_total_443",
                "bb_spm_443",
                "tsm",
                "chl_conc",
                "chiSquare",
                "K_min",
                "Z90_max",
                "Kd_490",
                "turbidity_index",
                "ac_flags",
                "case2_flags"
        };
        assertArrayEquals(expectedTargetBands, bandNames);
        ProductNodeGroup<Mask> maskGroup = c2rProduct.getMaskGroup();
        assertTrue(maskGroup.getNodeCount() > 0);
        assertTrue(maskGroup.contains("AC_INVALID"));
        assertTrue(maskGroup.contains("case2_wlr_oor"));
        assertTrue(maskGroup.contains("case2_whitecaps"));
        assertTrue(maskGroup.contains("case2_invalid"));

    }

    @Test
    public void testCoastColourNetCDFL1P() throws Exception {
        Product l1pProduct = getL1pProduct();
        try {
            GPF.createProduct("Meris.Case2Regional", GPF.NO_PARAMS, l1pProduct);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already contains a band with the name 'lat'")) {
                e.printStackTrace();
                fail("Exception is not expected: " + e.getMessage());
            }
        }
    }

    private static Product getL1bProduct() throws ParseException {
        int width = 10;
        int height = 10;
        Product product = new Product("MER_FR__1P", "MER_FR__1P", width, height);
        for (int i = 0; i < 15; i++) {
            product.addBand(String.format("radiance_%d", (i + 1)), ProductData.TYPE_UINT16).setSpectralBandIndex(i);

        }
        product.addBand("l1_flags", ProductData.TYPE_UINT8);
        product.addBand("detector_index", ProductData.TYPE_UINT16);
        float[] tiePointData = new float[width * height];
        product.addTiePointGrid(new TiePointGrid("sun_zenith", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("sun_azimuth", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("view_zenith", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("view_azimuth", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("dem_alt", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("atm_press", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("ozone", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("latitude", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("longitude", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("dem_rough", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("lat_corr", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("lon_corr", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("zonal_wind", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("merid_wind", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("rel_hum", width, height, 0, 0, 1, 1, tiePointData));

        FlagCoding l1_flags = new FlagCoding("l1_flags");
        l1_flags.addFlag("INVALID", 0x01, "No Description.");
        l1_flags.addFlag("LAND_OCEAN", 0x02, "No Description.");
        product.getBand("l1_flags").setSampleCoding(l1_flags);
        product.getFlagCodingGroup().add(l1_flags);
        product.setStartTime(ProductData.UTC.parse("12-Mar-2003 13:45:36"));
        product.setEndTime(ProductData.UTC.parse("12-Mar-2003 13:48:12"));
        final MetadataElement sph = new MetadataElement("SPH");
        final MetadataAttribute sphDescriptor = new MetadataAttribute("SPH_DESCRIPTOR",
                                                                      ProductData.createInstance(
                                                                              "MER_FR__1P SPECIFIC HEADER"), true);
        sph.addAttribute(sphDescriptor);
        product.getMetadataRoot().addElement(sph);
        return product;
    }

    private static Product getL1pProduct() throws ParseException {
        Product product = getL1bProduct();

        product.addBand("l1p_flags", ProductData.TYPE_UINT8);
        FlagCoding l1pFlagCoding = new FlagCoding("l1p_flags");
        l1pFlagCoding.addFlag("CC_LAND", 0x01, "No Description.");
        l1pFlagCoding.addFlag("CC_COASTLINE", 0x02, "No Description.");
        l1pFlagCoding.addFlag("CC_CLOUD", 0x04, "No Description.");
        product.getBand("l1p_flags").setSampleCoding(l1pFlagCoding);
        product.getFlagCodingGroup().add(l1pFlagCoding);
        Band lat = product.addBand("lat", ProductData.TYPE_FLOAT32);
        Band lon = product.addBand("lon", ProductData.TYPE_FLOAT32);
        product.setGeoCoding(new PixelGeoCoding(lat, lon, null, 5));
        return product;
    }


    private static Product getAtmoCorrectedProduct() throws ParseException {
        int width = 10;
        int height = 10;
        Product product = new Product("MERIS_L2_AC", "MERIS_L2_AC", width, height);
        for (int i = 0; i < 13; i++) {
            if (i == 10) {
                continue; // skip reflec_11 - it's not an output of AC
            }
            product.addBand(String.format("reflec_%d", (i + 1)), ProductData.TYPE_UINT16).setSpectralBandIndex(i);

        }
        product.addBand("ac_flags", ProductData.TYPE_UINT8);
        float[] tiePointData = new float[width * height];
        product.addTiePointGrid(new TiePointGrid("sun_zenith", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("sun_azimuth", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("view_zenith", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("view_azimuth", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("zonal_wind", width, height, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("merid_wind", width, height, 0, 0, 1, 1, tiePointData));

        FlagCoding agc_flags = new FlagCoding("ac_flags");
        agc_flags.addFlag("INVALID", 0x01, "No Description.");
        product.getBand("ac_flags").setSampleCoding(agc_flags);
        product.getFlagCodingGroup().add(agc_flags);
        product.addMask("AC_INVALID", "ac_flags.INVALID", "No Description.", Color.ORANGE, 0.4);

        product.setStartTime(ProductData.UTC.parse("12-Mar-2003 13:45:36"));
        product.setEndTime(ProductData.UTC.parse("12-Mar-2003 13:48:12"));
        final MetadataElement sph = new MetadataElement("SPH");
        final MetadataAttribute sphDescriptor = new MetadataAttribute("SPH_DESCRIPTOR",
                                                                      ProductData.createInstance(
                                                                              "MER_FR__1P SPECIFIC HEADER"), true);
        sph.addAttribute(sphDescriptor);
        product.getMetadataRoot().addElement(sph);
        return product;
    }

}
