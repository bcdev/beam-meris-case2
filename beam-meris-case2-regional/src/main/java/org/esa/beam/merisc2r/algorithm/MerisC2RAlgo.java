package org.esa.beam.merisc2r.algorithm;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.Auxdata;
import org.esa.beam.case2.algorithm.BandDescriptor;
import org.esa.beam.case2.algorithm.Case2Algorithm;
import org.esa.beam.case2.algorithm.Experimental;
import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.algorithm.PixelData;
import org.esa.beam.case2.algorithm.atmosphere.AtmosphereCorrection;
import org.esa.beam.case2.algorithm.atmosphere.Tosa;
import org.esa.beam.case2.algorithm.fit.ChiSquareFit;
import org.esa.beam.case2.algorithm.fit.ChiSquareFitGLM;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.merisc2r.algorithm.case2water.Case2Water;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MerisC2RAlgo extends Case2Algorithm {

    private static final double RL_TOA_THRESH_13 = 0.035;


    private AlgorithmParameter parameter;

    private Experimental experimental;
    private AtmosphereCorrection atmoCorrection;
    private Case2Water case2Water;
    private ChiSquareFit chiSquareFit;

    @Override
    public OutputBands init(Product inputProduct, String[] inputBandNames,
                            AlgorithmParameter parameter, Auxdata auxdata) {
        this.parameter = parameter;
        experimental = new Experimental(false);

        atmoCorrection = new AtmosphereCorrection();
        atmoCorrection.init(auxdata, parameter);

        case2Water = new Case2Water();
        case2Water.init(auxdata.getWaterNet(), auxdata.getForwardWaterNet(), parameter);
        chiSquareFit = new ChiSquareFitGLM();
        chiSquareFit.init(parameter, auxdata, createGLM());

        OutputBands outputBands = new OutputBands();
        outputBands.addDescriptor(createToaReflectanceDesrciptors(inputProduct, inputBandNames));
        outputBands.addDescriptor(createWaterReflectanceDesrciptors(inputProduct, inputBandNames));

        if (parameter.performAtmosphericCorrection) {
            outputBands.addDescriptor(createPathDesrciptors(inputProduct, inputBandNames));
            outputBands.addDescriptor(createTransmittanceDesrciptors(inputProduct, inputBandNames));
            outputBands.addDescriptor(createTosaReflectanceDesrciptors(inputProduct, inputBandNames));
            outputBands.addDescriptor(createAngstromDesrciptors());
        }

        outputBands.addDescriptor(createWaterDesrciptors());
        outputBands.addDescriptor(createFitDesrciptors());
        outputBands.addDescriptor(createFlagsDescriptor());
        return outputBands;
    }


    @Override
    public void perform(PixelData pixel, OutputBands outputBands) throws ProcessorException {

        double[] toaReflecs = outputBands.getDoubleValues("toa_reflec");
        System.arraycopy(pixel.toa_reflectance, 0, toaReflecs, 0, toaReflecs.length);
        outputBands.setValues("toa_reflec", toaReflecs);

        /* test for usable water pixel */
        if (!test_usable_waterpixel(pixel, outputBands)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.INVALID);
            return;
        }

        if (pixel.toa_reflectance[12] > RL_TOA_THRESH_13) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.RAD_ERR);
        }

        if (!validAncillaryData(pixel)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.ANCIL);
        }

        if (!validWindspeed(pixel)) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.WHITECAPS);
        }

        /* extract angles and ancillary data */
        double teta_view_deg = pixel.satzen; /* viewing zenith angle */
        final int centerPixel = outputBands.getProduct().getSceneRasterWidth() / 2;
        teta_view_deg = correctViewAngle(teta_view_deg, pixel.column, centerPixel, pixel.isFullResolution);
        double teta_sun_deg = pixel.solzen; /* sun zenith angle */
        double teta_view_rad = Math.toRadians(teta_view_deg);
        double teta_sun_rad = Math.toRadians(teta_sun_deg);


        double azi_diff_deg = getAzimuthDifference(pixel);
        if (parameter.performAtmosphericCorrection) {
            Tosa.Result tosa = atmoCorrection.perform(pixel, Math.toRadians(azi_diff_deg), teta_view_rad, teta_sun_rad,
                                                      outputBands);

            // estimated correction term for polarisation of path radiance
            // inactive because of new pol correction with NN
            experimental.doEstimatedPolCorr(tosa, outputBands);
            /* protection against too small RLw reflectances in blue spectral part */
            experimental.ensureValidBlueRlwReflectances(tosa, outputBands);
        } else {
            final int reflecLength = outputBands.getDoubleValues("reflec").length;
            outputBands.setValues("reflec_", Arrays.copyOf(pixel.toa_reflectance, reflecLength));
        }

        /* check if only atmospheric correction, then stop */
        if (shouldComputeC2W(parameter)) {
            double[] RLw_cut = case2Water.perform(teta_sun_deg, teta_view_deg, azi_diff_deg, outputBands);

            /*Doing the Fit:*/
            // should only be done if atmospheric correction succeeded,
            // but it is always out of range
            if (parameter.performChiSquareFit /*&& !acFailed*/) {
                chiSquareFit.perform(teta_sun_deg, teta_view_deg, azi_diff_deg, RLw_cut, outputBands);
            }
        } // end of processing a water pixel

    } /* end of retrieval and hope for success */

    private static boolean shouldComputeC2W(AlgorithmParameter parameter) {
        return parameter.outputAPig || parameter.outputAGelb || parameter.outputBTsm ||
               parameter.outputChlConc || parameter.outputTsmConc || parameter.outputOutOfScopeChiSquare ||
               parameter.performChiSquareFit;
    }

    private static double getAzimuthDifference(PixelData pixel) {
        double azi_diff_deg = Math.abs(pixel.satazi - pixel.solazi); /* azimuth difference */

        /* reverse azi difference */

        if (azi_diff_deg > 180.0) {
            azi_diff_deg = 360.0 - azi_diff_deg;
        }
        azi_diff_deg = 180.0 - azi_diff_deg; /* different definitions in MERIS data and MC /HL simulation */
        return azi_diff_deg;
    }

    private boolean test_usable_waterpixel(final PixelData pixel, OutputBands outputBands) {

        // check for ice or cloud
        if (pixel.cloudIceTerm.evalB(new RasterDataEvalEnv(pixel.column, pixel.row, 1, 1))) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.CLOUD_ICE);
            return false;
        }

        // check for land
        if (pixel.landWaterTerm.evalB(new RasterDataEvalEnv(pixel.column, pixel.row, 1, 1))) {
            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.LAND);
            return false;
        }

        return true;
    }


    /*---------------------------------------------------------------------------
    *test if ozone and atmospheric pressure is within valid range
    *Doerffer 20061106
    *--------------------------------------------------------------------------*/
    private static boolean validAncillaryData(PixelData pixel) {
        return !(pixel.ozone > 500 || pixel.ozone < 200 || pixel.pressure > 1100 || pixel.pressure < 500);
    }

    /*---------------------------------------------------------------------------
    *test if windspeed is above 10 m/s == beaufort 6, risk of whitecaps
    *Doerffer 20061107
    *--------------------------------------------------------------------------*/
    private static boolean validWindspeed(PixelData pixel) {
        return pixel.windspeed <= 12.0;
    }

    private BandDescriptor[] createWaterReflectanceDesrciptors(Product inputProduct, String[] inputBandNames) {
        BandDescriptor[] bandDescriptors = new BandDescriptor[parameter.outputWaterLeavingRefl.length];
        for (int i = 0; i < bandDescriptors.length; i++) {
            Band radBand = inputProduct.getBand(inputBandNames[i]);
            String description = MessageFormat.format("Water leaving radiance reflectance at {0} nm",
                                                      radBand.getSpectralWavelength());
            final boolean output = parameter.outputWaterLeavingRefl[i] && i != 10; // exclude band 11
            BandDescriptor bandDescriptor = createCommonDescriptor("reflec_" + (i + 1), "sr^-1", description,
                                                                   ProductData.TYPE_FLOAT32,
                                                                   false, output);
            if (parameter.switchToIrradianceReflectance) {
                bandDescriptor.setDescription("Water leaving irradiance reflectance at "
                                              + radBand.getSpectralWavelength() + " nm");
                bandDescriptor.setUnit("dl");
                bandDescriptor.setScalingFactor(Math.PI);
            }
            bandDescriptor.setSpectralWavelength(radBand.getSpectralWavelength());
            bandDescriptor.setSpectralBandwidth(radBand.getSpectralBandwidth());
            bandDescriptor.setSpectralBandIndex(radBand.getSpectralBandIndex());
            bandDescriptors[i] = bandDescriptor;
        }

        return bandDescriptors;
    }

    private BandDescriptor[] createToaReflectanceDesrciptors(Product inputProduct, String[] inputBandNames) {
        BandDescriptor[] bandDescriptors = new BandDescriptor[15];
        for (int i = 0; i < bandDescriptors.length; i++) {
            BandDescriptor bandDescriptor = createCommonDescriptor("toa_reflec_" + (i + 1), "sr^-1", "TOA Reflectance",
                                                                   ProductData.TYPE_FLOAT32,
                                                                   false, parameter.outputToaRefl[i]);
            Band radBand = inputProduct.getBand(inputBandNames[i]);
            bandDescriptor.setSpectralWavelength(radBand.getSpectralWavelength());
            bandDescriptor.setSpectralBandwidth(radBand.getSpectralBandwidth());
            bandDescriptor.setSpectralBandIndex(radBand.getSpectralBandIndex());
            bandDescriptors[i] = bandDescriptor;
        }

        return bandDescriptors;
    }

    private BandDescriptor[] createTosaReflectanceDesrciptors(Product inputProduct, String[] inputBandNames) {
        BandDescriptor[] bandDescriptors = new BandDescriptor[12];
        for (int i = 0; i < bandDescriptors.length; i++) {
            BandDescriptor bandDescriptor = createCommonDescriptor("tosa_reflec_" + (i + 1), "sr^-1",
                                                                   "TOSA Reflectance",
                                                                   ProductData.TYPE_FLOAT32,
                                                                   false, parameter.outputTosaRefl[i]);
            Band radBand = inputProduct.getBand(inputBandNames[i]);
            bandDescriptor.setSpectralWavelength(radBand.getSpectralWavelength());
            bandDescriptor.setSpectralBandwidth(radBand.getSpectralBandwidth());
            bandDescriptor.setSpectralBandIndex(radBand.getSpectralBandIndex());
            bandDescriptors[i] = bandDescriptor;
        }

        return bandDescriptors;
    }

    private BandDescriptor[] createPathDesrciptors(Product inputProduct, String[] inputBandNames) {
        BandDescriptor[] bandDescriptors = new BandDescriptor[parameter.outputPathRadianceRefl.length];
        for (int i = 0; i < bandDescriptors.length; i++) {
            final boolean output = parameter.outputPathRadianceRefl[i] && i != 10; // exclude band 11
            BandDescriptor bandDescriptor = createCommonDescriptor("path_" + (i + 1),
                                                                   "sr^-1", "Water leaving radiance reflectance path",
                                                                   ProductData.TYPE_FLOAT32,
                                                                   false, output);
            Band radBand = inputProduct.getBand(inputBandNames[i]);
            bandDescriptor.setSpectralWavelength(radBand.getSpectralWavelength());
            bandDescriptor.setSpectralBandwidth(radBand.getSpectralBandwidth());
            bandDescriptor.setSpectralBandIndex(radBand.getSpectralBandIndex());
            bandDescriptors[i] = bandDescriptor;
        }

        return bandDescriptors;
    }

    private BandDescriptor[] createTransmittanceDesrciptors(Product inputProduct, String[] inputBandNames) {
        BandDescriptor[] bandDescriptors = new BandDescriptor[parameter.outputTransmittance.length];
        for (int i = 0; i < bandDescriptors.length; i++) {
            final boolean output = parameter.outputTransmittance[i] && i != 10; // exclude band 11
            BandDescriptor bandDescriptor = createCommonDescriptor("trans_" + (i + 1),
                                                                   "dle",
                                                                   "Downwelling irrediance transmittance (Ed_Boa/Ed_Tosa)",
                                                                   ProductData.TYPE_FLOAT32,
                                                                   false, output);
            Band radBand = inputProduct.getBand(inputBandNames[i]);
            bandDescriptor.setSpectralWavelength(radBand.getSpectralWavelength());
            bandDescriptor.setSpectralBandwidth(radBand.getSpectralBandwidth());
            bandDescriptor.setSpectralBandIndex(radBand.getSpectralBandIndex());
            bandDescriptors[i] = bandDescriptor;
        }

        return bandDescriptors;
    }

    private BandDescriptor[] createAngstromDesrciptors() {
        return new BandDescriptor[]{
                createCommonDescriptor("tau_550", "dl", "Spectral aerosol optical depth",
                                       ProductData.TYPE_FLOAT32, false, parameter.outputTau),
                createCommonDescriptor("ang_443_865", "dl", "Aerosol Angstrom coefficient",
                                       ProductData.TYPE_FLOAT32, false, parameter.outputAngstrom)
        };
    }


    private BandDescriptor[] createWaterDesrciptors() {
        List<BandDescriptor> descriptorList = new ArrayList<BandDescriptor>(15);

        descriptorList.add(createCommonDescriptor("a_gelbstoff", "m^-1", "Gelbstoff absorbtion (A_Y) at 442 nm",
                                                  ProductData.TYPE_FLOAT32, true, parameter.outputAGelb));
        descriptorList.add(createCommonDescriptor("a_pig", "m^-1", "Pigment absorption at band 2 (A_PIG)",
                                                  ProductData.TYPE_FLOAT32, true, parameter.outputAPig));
        descriptorList.add(createCommonDescriptor("a_total", "m^-1", "Absorption at 443 nm of all water constituents",
                                                  ProductData.TYPE_FLOAT32, false, parameter.outputATotal));
        descriptorList.add(createCommonDescriptor("b_tsm", "m^-1", "Total supended matter scattering (B_TSM)",
                                                  ProductData.TYPE_FLOAT32, true, parameter.outputBTsm));
        descriptorList.add(
                createCommonDescriptor("tsm", "g m^-3", "Total supended matter dry weight concentration (TSM)",
                                       ProductData.TYPE_FLOAT32, true, parameter.outputTsmConc));
        descriptorList.add(createCommonDescriptor("chl_conc", "mg m^-3", "Chlorophyll concentration (CHL)",
                                                  ProductData.TYPE_FLOAT32, true, parameter.outputChlConc));
        descriptorList.add(createCommonDescriptor("chiSquare", null, "Chi Square Out of Scope",
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputOutOfScopeChiSquare));
        descriptorList.add(
                createCommonDescriptor("K_min", "m^-1", "Minimum downwelling irreadiance atenuation coefficient",
                                       ProductData.TYPE_FLOAT32, false, parameter.outputKmin));
        descriptorList.add(createCommonDescriptor("Z90_max", "m", "Maximum signal depth",
                                                  ProductData.TYPE_FLOAT32, false, parameter.outputZ90max));

        return descriptorList.toArray(new BandDescriptor[descriptorList.size()]);

    }

    private BandDescriptor[] createFitDesrciptors() {
        List<BandDescriptor> descriptorList = new ArrayList<BandDescriptor>(20);

        descriptorList.add(createCommonDescriptor("a_gelbstoffFit", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitAGelb && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("a_gelbstoffFit_max", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitAGelb && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("a_gelbstoffFit_min", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitAGelb && parameter.performChiSquareFit));

        descriptorList.add(createCommonDescriptor("a_pigFit", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitAPig && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("a_pigFit_max", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitAPig && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("a_pigFit_min", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitAPig && parameter.performChiSquareFit));

        descriptorList.add(createCommonDescriptor("b_tsmFit", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitBTsm && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("b_tsmFit_max", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitBTsm && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("b_tsmFit_min", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitBTsm && parameter.performChiSquareFit));

        descriptorList.add(createCommonDescriptor("tsmFit", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitTsmConc && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("chl_concFit", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputFitChlConc && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("chiSquareFit", null, null,
                                                  ProductData.TYPE_FLOAT32, true,
                                                  parameter.outputChiSquareFit && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("nIter", null, null,
                                                  ProductData.TYPE_INT32, false,
                                                  parameter.outputNIter && parameter.performChiSquareFit));
        descriptorList.add(createCommonDescriptor("paramChange", "1", "Parameter change in last fit step",
                                                  ProductData.TYPE_FLOAT32, false,
                                                  parameter.outputParamChange && parameter.performChiSquareFit));
        return descriptorList.toArray(new BandDescriptor[descriptorList.size()]);

    }

    private BandDescriptor createFlagsDescriptor() {
        BandDescriptor l2_flagsDescriptor = createCommonDescriptor("l2_flags", null, null, ProductData.TYPE_INT32,
                                                                   false, true);
        l2_flagsDescriptor.setInitialValue(0);
        l2_flagsDescriptor.setValidExpression("");
        return l2_flagsDescriptor;

    }

    private BandDescriptor createCommonDescriptor(String name, String unit, String description, int type,
                                                  boolean log10Scaled, boolean writeEnable) {
        BandDescriptor bandDescriptor = new BandDescriptor(name, description, type, unit, -1);
        bandDescriptor.setLog10Scaled(log10Scaled);
        bandDescriptor.setValidExpression("!l2_flags.INVALID");
        bandDescriptor.setWriteEnabled(writeEnable);
        return bandDescriptor;
    }

}
