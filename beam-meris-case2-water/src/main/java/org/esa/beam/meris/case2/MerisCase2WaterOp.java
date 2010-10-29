package org.esa.beam.meris.case2;

import org.esa.beam.case2.algorithm.MerisFlightDirection;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.PixelOperator;
import org.esa.beam.util.ProductUtils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.esa.beam.dataio.envisat.EnvisatConstants.*;

@OperatorMetadata(alias = "Meris.Case2Water",
                  description = "Performs IOP retrieval on atmospherically corrected MERIS products.",
                  authors = "Roland Doerffer (GKSS); Marco Peters (Brockmann Consult)",
                  copyright = "(c) 2010 by Brockmann Consult",
                  version = "1.0")
public class MerisCase2WaterOp extends PixelOperator {

    public static final int SOURCE_REFLEC_1_INDEX = 0;
    public static final int SOURCE_REFLEC_2_INDEX = 1;
    public static final int SOURCE_REFLEC_3_INDEX = 2;
    public static final int SOURCE_REFLEC_4_INDEX = 3;
    public static final int SOURCE_REFLEC_5_INDEX = 4;
    public static final int SOURCE_REFLEC_6_INDEX = 5;
    public static final int SOURCE_REFLEC_7_INDEX = 6;
    public static final int SOURCE_REFLEC_9_INDEX = 7;
    public static final int SOURCE_SOLAZI_INDEX = 8;
    public static final int SOURCE_SOLZEN_INDEX = 9;
    public static final int SOURCE_SATAZI_INDEX = 10;
    public static final int SOURCE_SATZEN_INDEX = 11;
    public static final int SOURCE_ZONAL_WIND_INDEX = 12;
    public static final int SOURCE_MERID_WIND_INDEX = 13;
    public static final int SOURCE_AGC_INVALID_INDEX = 14;

    public static final int TARGET_A_GELBSTOFF_INDEX = 0;
    public static final int TARGET_A_PIGMENT_INDEX = 1;
    public static final int TARGET_A_TOTAL_INDEX = 2;
    public static final int TARGET_B_TSM_INDEX = 3;
    public static final int TARGET_TSM_INDEX = 4;
    public static final int TARGET_CHL_CONC_INDEX = 5;
    public static final int TARGET_CHI_SQUARE_INDEX = 6;
    public static final int TARGET_K_MIN_INDEX = 7;
    public static final int TARGET_Z90_MAX_INDEX = 8;
    public static final int TARGET_FLAG_INDEX = 9;

    public static final int WLR_OOR_BIT_INDEX = 0;
    public static final int CONC_OOR_BIT_INDEX = 1;
    public static final int OOTR_BIT_INDEX = 2;
    public static final int WHITECAPS_BIT_INDEX = 3;
    public static final int INVALID_BIT_INDEX = 7;


    private static final String DEFAULT_INVERSE_WATER_NET_NAME = "meris_bn_20040322_45x16x12x8x5_5177.9.net";
    private static final String DEFAULT_FORWARD_WATER_NET_NAME = "meris_fn_20040319_15x15x15_1750.4.net";

    // todo - missing in EnvisatConstants
    private static final String MERIS_ZONAL_WIND_DS_NAME = "zonal_wind";
    private static final String MERIS_MERID_WIND_DS_NAME = "merid_wind";

    @SuppressWarnings({"PointlessBitwiseExpression"})
    private static final int WLR_OOR = 0x0001 << WLR_OOR_BIT_INDEX;         // WLR out of scope
    private static final int CONC_OOR = 0x01 << CONC_OOR_BIT_INDEX;         // concentration out of range
    private static final int OOTR = 0x01 << OOTR_BIT_INDEX;                 // out of training range == chi2 of measured and fwNN spectrum above threshold
    private static final int WHITECAPS = 0x01 << WHITECAPS_BIT_INDEX;       // risk for white caps
    private static final int INVALID = 0x01 << INVALID_BIT_INDEX;           // not a usable water pixel

    private static final String BAND_NAME_A_GELBSTOFF = "a_gelbstoff";
    private static final String BAND_NAME_A_PIGMENT = "a_pig";
    private static final String BAND_NAME_A_TOTAL = "a_total";
    private static final String BAND_NAME_B_TSM = "b_tsm";
    private static final String BAND_NAME_TSM = "tsm";
    private static final String BAND_NAME_CHL_CONC = "chl_conc";
    private static final String BAND_NAME_CHI_SQUARE = "chiSquare";
    private static final String BAND_NAME_K_MIN = "K_min";
    private static final String BAND_NAME_Z90_MAX = "Z90_max";
    private static final String BAND_NAME_CASE2_FLAGS = "case2_flags";

    private static final double WINDSPEED_THRESHOLD = 12.0;


    // todo - add required bands
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
    @Parameter(defaultValue = "4.0", description = "Threshold to indicate Spectrum is Out of Scope")
    private double spectrumOutOfScopeThreshold;

    @Parameter(defaultValue = "agc_flags.INVALID",
               description = "Expression defining pixels not considered for processing")
    private String invalidPixelExpression;

//    @Parameter(defaultValue = "agc_flags.LAND",
//               //defaultValue = "tosa_reflec_10 > tosa_reflec_6 AND tosa_reflec_13 > 0.0475",
//               description = "Expression for land-water-separation")
//    private String landWaterSeparationExpression;
//    // todo - changed expression from toa to tosa. Still correct or do we have to adopt values?
//    // maybe use a flag of source product?
//    @Parameter(defaultValue = "agc_flags.CLOUD_ICE", // defaultValue = "tosa_reflec_14 > 0.2",
//               description = "Expression for cloud-ice-detection")
//    private String cloudIceDetectionExpression;

    @Parameter(label = "Inverse water neural net (optional)",
               description = "The file of the inverse water neural net to be used instead of the default.")
    private File inverseWaterNnFile;
    @Parameter(label = "Forward water neural net (optional)",
               description = "The file of the forward water neural net to be used instead of the default.")
    private File forwardWaterNnFile;


    private int centerPixel;
    private boolean isFullResolution;
    private Case2Water case2Water;
    private String inverseWaterNnString;
    private String forwardWaterNnString;
    private ThreadLocal<NNffbpAlphaTabFast> threadLocalInverseWaterNet;
    private ThreadLocal<NNffbpAlphaTabFast> threadLocalforwardWaterNet;


    @Override
    protected void configureTargetProduct(Product targetProduct) {
        final Product sourceProduct = getSourceProduct();

        addTargetBand(targetProduct, BAND_NAME_A_GELBSTOFF, "m^-1",
                      "Gelbstoff (yellow substance) absorption  at 442 nm", true);
        addTargetBand(targetProduct, BAND_NAME_A_PIGMENT, "m^-1", "Pigment absorption at band 2 ", true);
        addTargetBand(targetProduct, BAND_NAME_A_TOTAL, "m^-1", "Absorption at 443 nm of all water constituents",
                      false);
        addTargetBand(targetProduct, BAND_NAME_B_TSM, "m^-1", "Total suspended matter scattering (B_TSM)", true);
        addTargetBand(targetProduct, BAND_NAME_TSM, "g m^-3", "Total suspended matter dry weight concentration (TSM)",
                      true);
        addTargetBand(targetProduct, BAND_NAME_CHL_CONC, "mg m^-3", "Chlorophyll concentration (CHL)", true);
        addTargetBand(targetProduct, BAND_NAME_CHI_SQUARE, null, "Chi Square Out of Scope", true);
        addTargetBand(targetProduct, BAND_NAME_K_MIN, "m^-1", "Minimum downwelling irreadiance attenuation coefficient",
                      false);
        addTargetBand(targetProduct, BAND_NAME_Z90_MAX, "m", "Maximum signal depth", false);


        // copy bands of FRS products
        ProductUtils.copyBand(MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME, sourceProduct, targetProduct);
        ProductUtils.copyBand(MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME, sourceProduct, targetProduct);
        ProductUtils.copyBand(MERIS_AMORGOS_L1B_ALTIUDE_BAND_NAME, sourceProduct, targetProduct);

        ProductUtils.copyFlagBands(sourceProduct, targetProduct);
        final Band[] sourceBands = sourceProduct.getBands();
        for (Band sourceBand : sourceBands) {
            if (sourceBand.isFlagBand()) {
                final Band targetBand = targetProduct.getBand(sourceBand.getName());
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }

        addFlagsAndMasks(targetProduct);


    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {
        configurator.defineSample(TARGET_A_GELBSTOFF_INDEX, BAND_NAME_A_GELBSTOFF);
        configurator.defineSample(TARGET_A_PIGMENT_INDEX, BAND_NAME_A_PIGMENT);
        configurator.defineSample(TARGET_A_TOTAL_INDEX, BAND_NAME_A_TOTAL);
        configurator.defineSample(TARGET_B_TSM_INDEX, BAND_NAME_B_TSM);
        configurator.defineSample(TARGET_TSM_INDEX, BAND_NAME_TSM);
        configurator.defineSample(TARGET_CHL_CONC_INDEX, BAND_NAME_CHL_CONC);
        configurator.defineSample(TARGET_CHI_SQUARE_INDEX, BAND_NAME_CHI_SQUARE);
        configurator.defineSample(TARGET_K_MIN_INDEX, BAND_NAME_K_MIN);
        configurator.defineSample(TARGET_Z90_MAX_INDEX, BAND_NAME_Z90_MAX);
        configurator.defineSample(TARGET_FLAG_INDEX, BAND_NAME_CASE2_FLAGS);
    }

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        final Product sourceProduct = getSourceProduct();
        configurator.defineSample(SOURCE_REFLEC_1_INDEX, MERIS_L2_REFLEC_1_BAND_NAME);
        configurator.defineSample(SOURCE_REFLEC_2_INDEX, MERIS_L2_REFLEC_2_BAND_NAME);
        configurator.defineSample(SOURCE_REFLEC_3_INDEX, MERIS_L2_REFLEC_3_BAND_NAME);
        configurator.defineSample(SOURCE_REFLEC_4_INDEX, MERIS_L2_REFLEC_4_BAND_NAME);
        configurator.defineSample(SOURCE_REFLEC_5_INDEX, MERIS_L2_REFLEC_5_BAND_NAME);
        configurator.defineSample(SOURCE_REFLEC_6_INDEX, MERIS_L2_REFLEC_6_BAND_NAME);
        configurator.defineSample(SOURCE_REFLEC_7_INDEX, MERIS_L2_REFLEC_7_BAND_NAME);
        configurator.defineSample(SOURCE_REFLEC_9_INDEX, MERIS_L2_REFLEC_9_BAND_NAME);
        configurator.defineSample(SOURCE_SOLAZI_INDEX, MERIS_SUN_AZIMUTH_DS_NAME);
        configurator.defineSample(SOURCE_SOLZEN_INDEX, MERIS_SUN_ZENITH_DS_NAME);
        configurator.defineSample(SOURCE_SATAZI_INDEX, MERIS_VIEW_AZIMUTH_DS_NAME);
        configurator.defineSample(SOURCE_SATZEN_INDEX, MERIS_VIEW_ZENITH_DS_NAME);
        configurator.defineSample(SOURCE_ZONAL_WIND_INDEX, MERIS_ZONAL_WIND_DS_NAME);
        configurator.defineSample(SOURCE_MERID_WIND_INDEX, MERIS_MERID_WIND_DS_NAME);
//        final VirtualBand landWaterBand = new VirtualBand("case2_land_water", ProductData.TYPE_INT8,
//                                                          sourceProduct.getSceneRasterWidth(),
//                                                          sourceProduct.getSceneRasterHeight(),
//                                                          landWaterSeparationExpression);
//        sourceProduct.addBand(landWaterBand);
//        configurator.defineSample(SOURCE_LAND_WATER_INDEX, landWaterBand.getName());
//        final VirtualBand cloudIceBand = new VirtualBand("case2_cloud_ice", ProductData.TYPE_INT8,
//                                                         sourceProduct.getSceneRasterWidth(),
//                                                         sourceProduct.getSceneRasterHeight(),
//                                                         cloudIceDetectionExpression);
//        sourceProduct.addBand(cloudIceBand);
//        configurator.defineSample(SOURCE_CLOUD_ICE_INDEX, cloudIceBand.getName());
        configurator.defineSample(SOURCE_AGC_INVALID_INDEX, "agc_invalid");

        // todo - what's correct?
        centerPixel = sourceProduct.getSceneRasterWidth() / 2;
//        centerPixel = new MerisFlightDirection(sourceProduct).getNadirColumnIndex();
        
        isFullResolution = !sourceProduct.getProductType().contains("RR");
        case2Water = new Case2Water(tsmConversionExponent, tsmConversionFactor,
                                    chlConversionFactor, chlConversionFactor, spectrumOutOfScopeThreshold);
        inverseWaterNnString = readNeuralNetString(DEFAULT_INVERSE_WATER_NET_NAME, inverseWaterNnFile);
        forwardWaterNnString = readNeuralNetString(DEFAULT_FORWARD_WATER_NET_NAME, forwardWaterNnFile);
        threadLocalInverseWaterNet = new ThreadLocal<NNffbpAlphaTabFast>() {
            @Override
            protected NNffbpAlphaTabFast initialValue() {
                try {
                    return new NNffbpAlphaTabFast(inverseWaterNnString);
                } catch (IOException e) {
                    throw new OperatorException("Not able to init neural net", e);
                }
            }
        };
        threadLocalforwardWaterNet = new ThreadLocal<NNffbpAlphaTabFast>() {
            @Override
            protected NNffbpAlphaTabFast initialValue() {
                try {
                    return new NNffbpAlphaTabFast(forwardWaterNnString);
                } catch (IOException e) {
                    throw new OperatorException("Not able to init neural net", e);
                }
            }
        };
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double solazi = sourceSamples[SOURCE_SOLAZI_INDEX].getDouble();
        final double satazi = sourceSamples[SOURCE_SATAZI_INDEX].getDouble();
        double azi_diff_deg = getAzimuthDifference(satazi, solazi);
        double solzen = sourceSamples[SOURCE_SOLZEN_INDEX].getDouble();
        double satzen = sourceSamples[SOURCE_SATZEN_INDEX].getDouble();
        satzen = correctViewAngle(satzen, x, centerPixel, isFullResolution);
        double zonalWind = sourceSamples[SOURCE_ZONAL_WIND_INDEX].getDouble();
        double meridWind = sourceSamples[SOURCE_MERID_WIND_INDEX].getDouble();
        double windspeed = Math.sqrt(zonalWind * zonalWind + meridWind * meridWind);
        if (sourceSamples[SOURCE_AGC_INVALID_INDEX].getBoolean()) {
            targetSamples[TARGET_FLAG_INDEX].set(INVALID_BIT_INDEX, true);
            return;
        }

        if (windspeed > WINDSPEED_THRESHOLD) {
            targetSamples[TARGET_FLAG_INDEX].set(WHITECAPS_BIT_INDEX, true);
        }

        NNffbpAlphaTabFast inverseWaterNet = threadLocalInverseWaterNet.get();
        NNffbpAlphaTabFast forwardWaterNet = threadLocalforwardWaterNet.get();
        case2Water.perform(inverseWaterNet, forwardWaterNet,
                           solzen, satzen, azi_diff_deg, sourceSamples, targetSamples);

    }

    // todo -remove
//    private boolean isCloudIceOrLand(Sample[] sourceSamples, WritableSample[] targetSamples) {
//        if (sourceSamples[SOURCE_CLOUD_ICE_INDEX].getBoolean()) { // check for ice or cloud
//            targetSamples[TARGET_FLAG_INDEX].set(CLOUD_ICE_BIT_INDEX, true);
//            return true;
//        }
//        if (sourceSamples[SOURCE_LAND_WATER_INDEX].getBoolean()) { // check for land
//            targetSamples[TARGET_FLAG_INDEX].set(LAND_BIT_INDEX, true);
//            return true;
//        }
//        return false;
//    }

    // todo - is it correct to use the center pixel? what about subsets?
    private double correctViewAngle(double satelliteZenith, int pixelX, int centerPixel, boolean isFullResolution) {
        final double ang_coef_1 = -0.004793;
        final double ang_coef_2 = isFullResolution ? 0.0093247 / 4 : 0.0093247;
        satelliteZenith = satelliteZenith + Math.abs(pixelX - centerPixel) * ang_coef_2 + ang_coef_1;
        return satelliteZenith;
    }

    /**
     * Computes the difference of solar and satellite azimuth in degree.
     *
     * @param satelliteAzimuth the satellite azimuth angle in degree
     * @param solarAzimuth     the solar azimuth angle in degree
     *
     * @return azimuth difference in degree
     */
    private static double getAzimuthDifference(double satelliteAzimuth, double solarAzimuth) {
        double azi_diff_deg = Math.abs(satelliteAzimuth - solarAzimuth); /* azimuth difference */
        /* reverse azi difference */
        if (azi_diff_deg > 180.0) {
            azi_diff_deg = 360.0 - azi_diff_deg;
        }
        azi_diff_deg = 180.0 - azi_diff_deg; /* different definitions in MERIS data and MC /HL simulation */
        return azi_diff_deg;
    }


    private void addFlagsAndMasks(Product targetProduct) {
        final FlagCoding case2FlagCoding = new FlagCoding(BAND_NAME_CASE2_FLAGS);
        case2FlagCoding.addFlag("WLR_OOR", WLR_OOR, "WLR out of scope");
        case2FlagCoding.addFlag("CONC_OOR", CONC_OOR, "concentration out of training range");
        case2FlagCoding.addFlag("OOTR", OOTR, "RLw out of training range");
        case2FlagCoding.addFlag("WHITECAPS", WHITECAPS, "Whitecaps pixels");
        case2FlagCoding.addFlag("INVALID", INVALID, "not valid");
        targetProduct.getFlagCodingGroup().add(case2FlagCoding);
        final Band case2Flags = targetProduct.addBand(BAND_NAME_CASE2_FLAGS, ProductData.TYPE_UINT8);
        case2Flags.setSampleCoding(case2FlagCoding);

        final ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();
        addMask(maskGroup, "c2w_wlr_oor", "WLR out of scope", "case2_flags.WLR_OOR", Color.CYAN, 0.5f);
        addMask(maskGroup, "c2w_conc_oor", "concentration out of training range", "case2_flags.CONC_OOR",
                Color.DARK_GRAY, 0.5f);
        addMask(maskGroup, "c2w_ootr", "RLw out of training range", "case2_flags.OOTR", Color.ORANGE, 0.5f);
        addMask(maskGroup, "c2w_whitecaps", "Whitecaps pixels", "case2_flags.WHITECAPS", Color.PINK, 0.5f);
        addMask(maskGroup, "c2w_invalid", "invalid case2 pixel", "case2_flags.INVALID", Color.RED, 0.0f);
    }

    private static void addMask(ProductNodeGroup<Mask> maskGroup, String name, String description,
                                String expression, Color color, float transparency) {
        maskGroup.add(Mask.BandMathsType.create(name, description,
                                                maskGroup.getProduct().getSceneRasterWidth(),
                                                maskGroup.getProduct().getSceneRasterHeight(),
                                                expression, color, transparency));
    }

    private void addTargetBand(Product targetProduct, String bandName, String unit, String description,
                               boolean log10Scaled) {
        final Band band = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        band.setDescription(description);
        band.setUnit(unit);
        band.setLog10Scaled(log10Scaled);
        band.setValidPixelExpression("!case2_flags.INVALID");
    }

    private String readNeuralNetString(String resourceNetName, File neuralNetFile) {
        InputStream neuralNetStream;
        if (neuralNetFile == null) {
            neuralNetStream = getClass().getResourceAsStream(resourceNetName);
        } else {
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                neuralNetStream = new FileInputStream(neuralNetFile);
            } catch (FileNotFoundException e) {
                throw new OperatorException(e);
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(neuralNetStream));
        try {
            String line = reader.readLine();
            final StringBuilder sb = new StringBuilder();
            while (line != null) {
                // have to append line terminator, cause it's not included in line
                sb.append(line).append('\n');
                line = reader.readLine();
            }
            return sb.toString();
        } catch (IOException ioe) {
            throw new OperatorException("Could not initialize neural net", ioe);
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisCase2WaterOp.class);
        }
    }


}
