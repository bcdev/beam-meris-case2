package org.esa.beam.lakes.eutrophic.processor;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.EvalEnv;
import com.bc.jexp.EvalException;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.AbstractSymbol;
import com.bc.jexp.impl.ParserImpl;
import org.esa.beam.case2.algorithm.BandDescriptor;
import org.esa.beam.case2.algorithm.Flags;
import org.esa.beam.case2.algorithm.OutputBands;
import org.esa.beam.case2.algorithm.PixelData;
import org.esa.beam.case2.algorithm.Auxdata;
import org.esa.beam.case2.util.ObjectIO;
import org.esa.beam.case2.util.RasterBlockMap;
import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;
import org.esa.beam.case2.processor.ReadMePage;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.IOParameterPage;
import org.esa.beam.framework.processor.ui.MultiPageProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.framework.processor.ui.PropertyFileParameterPage;
import org.esa.beam.lakes.eutrophic.algorithm.EutrophicAlgorithmParameter;
import org.esa.beam.lakes.eutrophic.algorithm.EutrophicLakesAlgo;
import org.esa.beam.lakes.eutrophic.algorithm.fit.FitReflCutRestrConcs_v3;
import org.esa.beam.processor.smile.SmileAuxData;
import org.esa.beam.util.ProductUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.net.URL;


public class EutrophicLakesProcessor extends Processor {

    private Product inputProduct;
    private Term validTerm;
    private Product outputProduct;
    private Logger logger;

    // grid names
    private static final String latitude = EnvisatConstants.MERIS_LAT_DS_NAME;
    private static final String longitude = EnvisatConstants.MERIS_LON_DS_NAME;
    private static final String sun_zenith = EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME;
    private static final String sun_azimuth = EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME;
    private static final String view_zenith = EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME;
    private static final String view_azimuth = EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME;
    private static final String ozone = "ozone";
    private static final String atm_press = "atm_press";
    private static final String dem_alt = "dem_alt";
    private static final String zonal_wind = "zonal_wind";
    private static final String merid_wind = "merid_wind";
    private static final String rel_hum = "rel_hum";
    private static final String l1_flags = EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME;
    private static final String detectorIndex = EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME;
    private static final String[] inputGridNames = new String[]{
            latitude, longitude, sun_zenith, sun_azimuth,
            view_zenith, view_azimuth, ozone, atm_press, dem_alt,
            zonal_wind, merid_wind, rel_hum, l1_flags
    };
    private String[] inputBandNames;

    private static final int LINES_PER_BLOCK = 100;
    private final RasterBlockMap inputRasterBlocks = new RasterBlockMap(LINES_PER_BLOCK);
    private final RasterBlockMap outputRasterBlocks = new RasterBlockMap(LINES_PER_BLOCK);

    private EutrophicLakesAlgo algo;
    private File auxdataDir;
    private EutrophicAlgorithmParameter parameter;
    private static final BitmaskDef[] bitmaskDefs = new BitmaskDef[]{
            new BitmaskDef("l2_land", "land pixels", "l2_flags.LAND", Color.GREEN, 0.5f),
            new BitmaskDef("cloud_ice", "cloud or ice pixels", "l2_flags.CLOUD_ICE", Color.YELLOW, 0.5f),
            new BitmaskDef("ancil", "missing/OOR auxiliary data", "l2_flags.ANCIL", Color.GRAY, 0.5f),
            new BitmaskDef("solzen", "large solar zenith angle", "l2_flags.SOLZEN", Color.LIGHT_GRAY, 0.5f),
            new BitmaskDef("satzen", "large spacecraft zenith angle", "l2_flags.SATZEN", Color.LIGHT_GRAY, 0.5f),
            new BitmaskDef("whitecaps", "Whitecaps pixels", "l2_flags.WHITECAPS", Color.PINK, 0.5f),
            new BitmaskDef("rad_err", "TOAR out of valid range", "l2_flags.RAD_ERR", Color.MAGENTA, 0.5f),
            new BitmaskDef("tosa_oor", "TOSA out of range", "l2_flags.TOSA_OOR", Color.BLUE, 0.5f),
            new BitmaskDef("wlr_oor", "WLR out of scope", "l2_flags.WLR_OOR", Color.CYAN, 0.5f),
            new BitmaskDef("ootr", "RLw out of training range", "l2_flags.OOTR", Color.ORANGE, 0.5f),
            new BitmaskDef("l2_invalid", "invalid L2 product",
                           "l2_flags.OOTR || l2_flags.WLR_OOR || l2_flags.TOSA_OOR || l2_flags.LAND || l2_flags.CLOUD_ICE || l2_flags.RAD_ERR || l2_flags.WHITECAPS",
                           Color.RED, 0.0f),
            new BitmaskDef("atc_oor", "atmos. correct. out of range", "l2_flags.ATC_OOR", Color.LIGHT_GRAY, 0.5f),
            new BitmaskDef("conc_oor", "concentration out of training range", "l2_flags.CONC_OOR", Color.DARK_GRAY,
                           0.5f),
            // the following are not used
            new BitmaskDef("sunglint", "sunglint risk", "l2_flags.SUNGLINT", Color.BLACK, 0.5f),
            new BitmaskDef("fitFailed", "fit passed threshold", "l2_flags.FIT_FAILED", Color.ORANGE, 0.5f),
            new BitmaskDef("spareflag06", "spare flag 06", "l2_flags.SPAREFLAG06", Color.BLACK, 0.5f),
            new BitmaskDef("spareflag07", "spare flag 07", "l2_flags.SPAREFLAG07", Color.BLACK, 0.5f),
    };
    private Auxdata auxdata;
    private OutputBands outputBands;

    /**
     * Creates an instance of Meris Case-2 Regional processor
     */
    public EutrophicLakesProcessor() {
        logger = Logger.getLogger(EutrophicLakesConstants.PROCESSOR_LOGGER_NAME);
        setDefaultHelpId(EutrophicLakesConstants.PROCESSOR_HELP_ID);        
    }

    /**
     * Creates the UI for the processor. Override to perform processor specific UI initializations.
     */
    @Override
    public ProcessorUI createUI() throws
                                  ProcessorException {

        final IOParameterPage ioPage = new IOParameterPage();
        ioPage.setDefaultOutputProductFileName(EutrophicLakesConstants.DEFAULT_OUPUT_FILE_NAME);
        ioPage.setDefaultLogPrefix(EutrophicLakesConstants.DEFAULT_LOG_PREFIX);
        ioPage.setDefaultLogToOutputParameter(EutrophicLakesConstants.DEFAULT_LOG_TO_OUTPUT);

        final File parameterFile = new File(auxdataDir, EutrophicLakesConstants.DEFAULT_PARAMETER_FILE_NAME);

        final PropertyFileParameterPage propertyFilePage = new PropertyFileParameterPage(parameterFile);

        final MultiPageProcessorUI processorUI = new MultiPageProcessorUI(
                EutrophicLakesConstants.PROCESSING_REQUEST_TYPE);
        final URL url = getClass().getResource("/" + EutrophicLakesConstants.README_FILE_NAME);
        processorUI.addPage(new ReadMePage(url));
        processorUI.addPage(ioPage);
        processorUI.addPage(propertyFilePage);
        return processorUI;
    }

    /**
     * Initializes the processor. Override to perform processor specific initialization. Called by the framework after
     * the logging is initialized.
     */
    @Override
    public void initProcessor() throws ProcessorException {
        File defaultAuxdataInstallDir = getDefaultAuxdataInstallDir();
        defaultAuxdataInstallDir = new File(defaultAuxdataInstallDir, EutrophicLakesConstants.PROCESSOR_VERSION);
        setAuxdataInstallDir(EutrophicLakesConstants.AUXDATA_DIR_PROPERTY, defaultAuxdataInstallDir);
        installAuxdata();
        auxdataDir = getAuxdataInstallDir();
    }

    private void loadAuxdata() throws ProcessorException {
        parameter.waterNnInverseFilePath = convertToAbsolutepath(auxdataDir, parameter.waterNnInverseFilePath);
        parameter.waterNnForwardFilePath = convertToAbsolutepath(auxdataDir, parameter.waterNnForwardFilePath);
        parameter.atmCorrNnFilePath = convertToAbsolutepath(auxdataDir, parameter.atmCorrNnFilePath);
        parameter.polCorrNnFilePath = convertToAbsolutepath(auxdataDir, parameter.polCorrNnFilePath);

        SmileAuxData smileAuxdata = null;
        try {
            if (parameter.performSmileCorrection) {
                File smileAuxdataDir = new File(auxdataDir, parameter.smileAuxdataDirPath);
                if (isProductFullResoultion(inputProduct)) {
                    smileAuxdata = SmileAuxData.loadFRAuxData(smileAuxdataDir);
                } else {
                    smileAuxdata = SmileAuxData.loadRRAuxData(smileAuxdataDir);
                }
            }
        } catch (IOException e) {
            throw new ProcessorException("Failed to load smile auxiliary dataset", e);
        }

        try {
            final NNffbpAlphaTabFast inverseWaterNet = new NNffbpAlphaTabFast(parameter.waterNnInverseFilePath);
            final NNffbpAlphaTabFast forwardWaterNet = new NNffbpAlphaTabFast(parameter.waterNnForwardFilePath);
            final NNffbpAlphaTabFast atmosphericNet = new NNffbpAlphaTabFast(parameter.atmCorrNnFilePath);
            final NNffbpAlphaTabFast polarizationNet = new NNffbpAlphaTabFast(parameter.polCorrNnFilePath);
            final FitReflCutRestrConcs_v3 fitReflCutRestrConcs_v3 = new FitReflCutRestrConcs_v3(parameter.fitCut,
                                                                                                parameter, 1.0);
            auxdata = new Auxdata(inverseWaterNet, forwardWaterNet,
                                  atmosphericNet, polarizationNet,
                                  smileAuxdata, fitReflCutRestrConcs_v3);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ProcessorException("Failed to load auxiliary dataset", e);
        }
    }

    private static String convertToAbsolutepath(File dir, String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(dir, file.getPath());
        }
        path = file.getPath();
        return path;
    }

    private void loadParameter(File parameterFile) throws ProcessorException {
        final String parameterFilePath = parameterFile.getPath();
        try {
            parameter = ObjectIO.readObject(EutrophicAlgorithmParameter.class, parameterFilePath);
        } catch (IOException e) {
            throw new ProcessorException("Failed to load parameter file " + parameterFilePath +
                                         "\n\n" + e.getMessage(), e);
        }
    }

    /**
     * Worker method invoked by framework to process a single request.
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        try {
            logger.info("Started processing ...");

            final Request request = getRequest();
            // check the request type
            Request.checkRequestType(request, EutrophicLakesConstants.PROCESSING_REQUEST_TYPE);
            //init algorithm
            final String paramName = EutrophicLakesConstants.PROPERTY_FILE_PARAM_NAME;
            final Object paramValue = request.getParameter(paramName).getValue();
            final File paramFile;
            if (paramValue instanceof File) {
                paramFile = (File) paramValue;
            } else if (paramValue instanceof String) {
                paramFile = new File((String) paramValue);
            } else {
                throw new ProcessorException("Failed to detect parameter file");
            }

            pm.beginTask("Processing request...", 200);

            try {
                loadParameter(paramFile);
                pm.worked(1);
                // load input product
                loadInputProduct();
                pm.worked(3);

                loadAuxdata();
                pm.worked(1);

                algo = new EutrophicLakesAlgo();
                outputBands = algo.init(inputProduct, inputBandNames, parameter, auxdata);
                if (pm.isCanceled()) {
                    return;
                }

                // create the output product
                createOutputProduct(new SubProgressMonitor(pm, 5));
                if (pm.isCanceled()) {
                    return;
                }

                // and process the MacNN1
                processCase2(new SubProgressMonitor(pm, 190));
            }catch(Throwable e) {
                e.printStackTrace();
                throw new ProcessorException(e.getMessage(), e);
            } finally {
                pm.done();
            }

            logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ProcessorException(e.getMessage());
        } finally {
            if (outputProduct != null) {
                outputProduct.dispose();
            }
            outputRasterBlocks.clear();

            if (inputProduct != null) {
                inputProduct.dispose();
            }
            inputRasterBlocks.clear();
        }
    }

    /**
     * Retrieves the name of the processor
     */
    @Override
    public String getName() {
        return EutrophicLakesConstants.PROCESSOR_NAME;
    }

    /**
     * Retrieves a version string of the processor
     */
    @Override
    public String getVersion() {
        return EutrophicLakesConstants.PROCESSOR_VERSION;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public String getCopyrightInformation() {
        return EutrophicLakesConstants.PROCESSOR_COPYRIGHT_INFO;
    }

    /*
     * Loads the input product from the request. Opens the product and opens both bands needed to process the ndvi.
     */
    private void loadInputProduct() throws ProcessorException, IOException {
        inputProduct = loadInputProduct(0);
        validTerm = ProcessorUtils.createTerm(parameter.inputValidMask, inputProduct);
        loadInputRasters();
    }

    private void loadInputRasters() throws
                                    ProcessorException {
        final String[] requiredRasterNames = getRequiredRasterNames();
        for (String inputRasterName : requiredRasterNames) {
            final RasterDataNode raster = inputProduct.getRasterDataNode(inputRasterName);
            if (raster == null) {
                throw new ProcessorException("Cannot load required raster " + inputRasterName);
            }
            inputRasterBlocks.addRaster(raster);
            logger.info(ProcessorConstants.LOG_MSG_LOADED_BAND + inputRasterName);
        }
    }

    private String[] getRequiredRasterNames() {
        List<String> rasterNames = new ArrayList<String>(27);
        rasterNames.addAll(Arrays.asList(inputGridNames));
        if (parameter.performAtmosphericCorrection) {
            inputBandNames = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;
            rasterNames.add(detectorIndex);
        } else {
            inputBandNames = new String[]{
                    EnvisatConstants.MERIS_L2_REFLEC_1_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_2_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_3_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_4_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_5_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_6_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_7_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_8_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_9_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_10_BAND_NAME,
                    "reflec_11",
                    EnvisatConstants.MERIS_L2_REFLEC_12_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_13_BAND_NAME,
                    EnvisatConstants.MERIS_L2_REFLEC_14_BAND_NAME,
                    "reflec_15"
            };
        }
        rasterNames.addAll(Arrays.asList(inputBandNames));
        return rasterNames.toArray(new String[rasterNames.size()]);
    }

    private static boolean isProductFullResoultion(final Product product) {
        final String productType = product.getProductType();
        return productType.contains("FR") || productType.contains("FSG");
    }


    /*
      Creates the output product skeleton.
     */
    private void createOutputProduct(ProgressMonitor pm) throws ProcessorException, IOException {
        // get the request from the base class
        final Request request = getRequest();

        // get the scene size from the input product
        final int sceneWidth = inputProduct.getSceneRasterWidth();
        final int sceneHeight = inputProduct.getSceneRasterHeight();

        // get the output product from the request. The request holds objects of
        // type ProductRef which contain all the information needed here
        // the request can contain any number of output products, we take the first ..
        final ProductRef outputRef = request.getOutputProductAt(0);
        if (outputRef == null) {
            throw new ProcessorException("No output product in request");
        }

        // create the in memory represenation of the output product the product itself
        outputProduct = new Product(new File(outputRef.getFilePath()).getName(), EutrophicLakesConstants.OUTPUT_PRODUCT_TYPE,
                                    sceneWidth,
                                    sceneHeight);

        pm.beginTask("Initializing output product..", 100);
        try {

            createOutputBands();
            pm.worked(15);                          // pm-sum 15

            copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME, inputProduct, outputProduct);
            copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME, inputProduct, outputProduct);
            copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_ALTIUDE_BAND_NAME, inputProduct, outputProduct);

            copyTiePointGridsToOutput();
            pm.worked(30);                          // pm-sum 45
            copyFlagBands(inputProduct, outputProduct);
            pm.worked(15);                          // pm-sum 60
            // copy the geocoding from input to output. The geocoding
            // tells the product which tiepoint grids define the geolocation
            copyGeoCoding(inputProduct, outputProduct);
            pm.worked(10);                          // pm-sum 70
            setL2FlagsToOutput();
            pm.worked(5);                           // pm-sum 75
            addBitmasksToOutput();
            pm.worked(5);                           // pm-sum 80
            addMetadataToProduct();

            // retrieve the default disk writer from the ProductIO package
            // this is the BEAM_DIMAP format, the toolbox native file format
            // and attach to the writer to the output product
            final ProductWriter writer = ProductIO.getProductWriter(outputRef.getFileFormat());
            outputProduct.setProductWriter(writer);
            outputProduct.setFileLocation(new File(outputRef.getFilePath()));

            // and initialize the disk represenation
            writer.writeProductNodes(outputProduct, outputProduct.getFileLocation());

            // l1_flags must be written
            writeL1FlagsToOutput(new SubProgressMonitor(pm, 15));       // pm-sum 95
            final String[] namesToCopy = getBandNamesToCopy();
            copyBandData(namesToCopy, inputProduct, outputProduct, new SubProgressMonitor(pm, 5));  // pm-sum 100
        } finally {
            pm.done();
        }

        logger.info("Output product initialized");
    }

    private void createOutputBands() {
        BandDescriptor[] bandDescriptors = outputBands.getAllDescriptors();
        for (BandDescriptor bandDescriptor : bandDescriptors) {
            addBand(bandDescriptor);
        }
        outputBands.setProduct(outputProduct);
    }

    private void addBand(BandDescriptor descriptor) {
        if (descriptor.isWriteEnabled()) {
            final Band band = outputProduct.addBand(descriptor.getName(), descriptor.getType());
            band.setUnit(descriptor.getUnit());
            band.setDescription(descriptor.getDescription());
            band.setValidPixelExpression(descriptor.getValidExpression());
            band.setScalingFactor(descriptor.getScalingFactor());
            band.setLog10Scaled(descriptor.isLog10Scaled());
            band.setSpectralBandIndex(descriptor.getSpectralBandIndex());
            band.setSpectralWavelength(descriptor.getSpectralWavelength());
            band.setSpectralBandwidth(descriptor.getSpectralBandwidth());
            outputRasterBlocks.addRaster(band, descriptor.getInitialValue());
        }
    }

    private void writeL1FlagsToOutput(ProgressMonitor pm) throws
                                                          IOException {
        final Band srcFlagsBand = inputProduct.getBand("l1_flags");
        final Band destFlagsBand = outputProduct.getBand("l1_flags");

        pm.beginTask("Writing L1 flags...", 2);
        try {
            if (destFlagsBand != null && srcFlagsBand != null) {
                srcFlagsBand.readRasterDataFully(new SubProgressMonitor(pm, 1));
                destFlagsBand.setRasterData(srcFlagsBand.getRasterData());
                destFlagsBand.writeRasterDataFully(new SubProgressMonitor(pm, 1));
            }
        } finally {
            pm.done();
        }
    }

    private void addBitmasksToOutput() {
        for (BitmaskDef bitmaskDef : bitmaskDefs) {
            // need a copy, cause the BitmaskDefs are otherwise disposed
            // if the outputProduct gets disposed after processing
            outputProduct.addBitmaskDef(bitmaskDef.createCopy());
        }
    }

    private void setL2FlagsToOutput() {
        final FlagCoding flagCoding = new FlagCoding("l2_flags");
        flagCoding.addFlag("RAD_ERR", Flags.RAD_ERR, "TOAR out of valid range"); // not used
        flagCoding.addFlag("LAND", Flags.LAND, "land pixels");
        flagCoding.addFlag("CLOUD_ICE", Flags.CLOUD_ICE, "cloud or ice");
        flagCoding.addFlag("SUNGLINT", Flags.SUNGLINT, "sunglint risk"); // not used
        flagCoding.addFlag("ANCIL", Flags.ANCIL, "missing/OOR auxiliary data");  // not used
        flagCoding.addFlag("TOSA_OOR", Flags.TOSA_OOR, "TOSA out of range");
        flagCoding.addFlag("WLR_OOR", Flags.WLR_OOR, "WLR out of scope");
        flagCoding.addFlag("SOLZEN", Flags.SOLZEN, "large solar zenith angle");
        flagCoding.addFlag("SATZEN", Flags.SATZEN, "large spacecraft zenith angle"); // not used
        flagCoding.addFlag("ATC_OOR", Flags.ATC_OOR, "atmos. correct. out of range");
        flagCoding.addFlag("CONC_OOR", Flags.CONC_OOR, "concentration out of training range");
        flagCoding.addFlag("OOTR", Flags.OOTR, "RLw out of training range");
        flagCoding.addFlag("WHITECAPS", Flags.WHITECAPS, "Whitecaps pixels");
        flagCoding.addFlag("FIT_FAILED", Flags.FIT_FAILED, "fit failed");
        flagCoding.addFlag("SPAREFLAG06", Flags.SPAREFLAG06, "spare flag 06");   // not used
        flagCoding.addFlag("SPAREFLAG07", Flags.SPAREFLAG07, "spare flag 07");   // not used
        flagCoding.addFlag("INVALID", Flags.INVALID, "not valid");
        outputProduct.addFlagCoding(flagCoding);
        outputProduct.getBand("l2_flags").setFlagCoding(flagCoding);
    }

    private void copyTiePointGridsToOutput() {
        for (String inputGridName : inputGridNames) {
            ProductUtils.copyTiePointGrid(inputGridName, inputProduct, outputProduct);
        }
    }

    private void addMetadataToProduct() {
        final MetadataElement processorMetadata = getProcessorMetadata();
        final MetadataElement sourceProductMetadata = new MetadataElement("Source Product");
        ProductUtils.copyMetadata(inputProduct.getMetadataRoot(), sourceProductMetadata);
        processorMetadata.addElement(sourceProductMetadata);
        processorMetadata.addElement(ObjectIO.getObjectMetadata(parameter));
        processorMetadata.addElement(getRequest().convertToMetadata());
        outputProduct.getMetadataRoot().addElement(processorMetadata);
    }

    /*
     * Performs the actual processing of the output product. Reads both input bands line by line, calculates the ndvi
     * and writes the result to the output band
     */
    private void processCase2(ProgressMonitor pm) throws IOException, ProcessorException {

        final PixelData pixel = new PixelData();

        // first of all - allocate memory for a single scan line
        final int width = inputProduct.getSceneRasterWidth();
        final int height = inputProduct.getSceneRasterHeight();

        for (int i = 0; i < inputBandNames.length; i++) {
            pixel.solar_flux[i] = ((Band) inputRasterBlocks.getRaster(inputBandNames[i])).getSolarFlux();
        }

        pixel.isFullResolution = isProductFullResoultion(inputProduct);

        ToaReflecSymbol[] landWaterSymbols = initializeTerms(pixel);

        pm.beginTask("Processing MERIS Level-1b Pixels...", height);

        try {
            // now loop over all scanlines
            for (int y = 0; y < height; y += inputRasterBlocks.getLinesPerBlock()) {
                if (pm.isCanceled()) {
                    return;
                }

                // read the input data
                int linesPerBlock = inputRasterBlocks.readBlock(y);
                //read inputValidMask
                final boolean[] validLinePixels = new boolean[width * linesPerBlock];

                inputProduct.readBitmask(0, y, width, linesPerBlock, validTerm, validLinePixels, ProgressMonitor.NULL);

                for (int by = 0; by < linesPerBlock; by++) {

                    pixel.row = y + by;

                    // process the complete scanline
                    for (int x = 0; x < width; x++) {
                        if (pm.isCanceled()) {
                            return;
                        }

                        int pixelIndex = x + by * width;
                        if (validLinePixels[pixelIndex]) {
                            pixel.column = x;
                            PixelPos pixelPos = new PixelPos(x, pixel.row);
                            GeoPos geoPos = inputProduct.getGeoCoding().getGeoPos(pixelPos, null);
                            pixel.lat = geoPos.lat;
                            pixel.lon = geoPos.lon;
                            pixel.altitude = inputRasterBlocks.getPixelFloat(dem_alt, pixelIndex);
                            pixel.solzen = inputRasterBlocks.getPixelFloat(sun_zenith, pixelIndex);
                            pixel.solazi = inputRasterBlocks.getPixelFloat(sun_azimuth, pixelIndex);
                            pixel.satzen = inputRasterBlocks.getPixelFloat(view_zenith, pixelIndex);
                            pixel.satazi = inputRasterBlocks.getPixelFloat(view_azimuth, pixelIndex);
                            pixel.ozone = inputRasterBlocks.getPixelFloat(ozone, pixelIndex);
                            pixel.pressure = inputRasterBlocks.getPixelFloat(atm_press, pixelIndex);


                            double zonalWind = inputRasterBlocks.getPixelFloat(zonal_wind, pixelIndex);
                            double meridWind = inputRasterBlocks.getPixelFloat(merid_wind, pixelIndex);
                            pixel.windspeed = Math.sqrt(zonalWind * zonalWind + meridWind * meridWind);

                            if(parameter.performAtmosphericCorrection) {
                                pixel.detectorIndex = inputRasterBlocks.getPixelInt(detectorIndex, pixelIndex);
                            for (int ib = 0; ib < inputBandNames.length; ib++) {
                                final String inputBandName = inputBandNames[ib];
                                pixel.toa_radiance[ib] = inputRasterBlocks.getPixelFloat(inputBandName, pixelIndex);
                                if(EnvisatConstants.MERIS_L1B_RADIANCE_1_BAND_NAME.equals(inputBandName)) {
                                    pixel.toa_radiance[ib] *= parameter.radiance1AdjustmentFactor;
                                }

                                pixel.toa_reflectance[ib] = pixel.toa_radiance[ib] / (pixel.solar_flux[ib] * Math.cos(
                                        Math.toRadians(pixel.solzen)));
                            }
                            } else {
                                for (int ib = 0; ib < inputBandNames.length; ib++) {
                                    pixel.toa_reflectance[ib] = inputRasterBlocks.getPixelFloat(inputBandNames[ib], pixelIndex);
                                }
                            }
                            for (int i = 0; i < landWaterSymbols.length; i++) {
                                ToaReflecSymbol landWaterSymbol = landWaterSymbols[i];
                                landWaterSymbol.setValue(pixel.toa_reflectance[i]);
                            }
                            algo.perform(pixel, outputBands);
                        } else {
                            outputBands.setValue("l2_flags", outputBands.getIntValue("l2_flags") | Flags.INVALID);
                        }

                        BandDescriptor[] bandDescriptors = outputBands.getAllDescriptors();
                        for (BandDescriptor bandDescriptor : bandDescriptors) {
                            double value = bandDescriptor.getValue().getElemDouble();
                            if (bandDescriptor.isLog10Scaled()) {
                                value = applyLog10(value);
                            }
                            outputRasterBlocks.setPixel(bandDescriptor.getName(), pixelIndex, value);
                            bandDescriptor.setDoubleValue(bandDescriptor.getInitialValue());      // resets the value
                        }
                    }
                    pm.worked(1);
                }
                // write the result
                outputRasterBlocks.writeBlock(y);
            }
        } finally {
            pm.done();
        }

    }

    private ToaReflecSymbol[] initializeTerms(PixelData pixel) throws ProcessorException {
        WritableNamespace landWaterNamespace = outputProduct.createBandArithmeticDefaultNamespace();
        final ParserImpl parser = new ParserImpl(landWaterNamespace, false);
        ToaReflecSymbol[] landWaterSymbols = new ToaReflecSymbol[pixel.toa_reflectance.length];
        for (int i = 0; i < pixel.toa_reflectance.length; i++) {
            final ToaReflecSymbol symbol = new ToaReflecSymbol("toa_reflec_" + (i + 1));
            landWaterNamespace.registerSymbol(symbol);
            landWaterSymbols[i] = symbol;
        }
        try {
            pixel.cloudIceTerm = parser.parse(parameter.cloudIceDetectionExpression);
            pixel.landWaterTerm = parser.parse(parameter.landWaterSeparationExpression);
        } catch (ParseException e) {
            e.printStackTrace();
            final String message = "Could not parse expression for land/water separation: " +
                                   parameter.landWaterSeparationExpression;
            throw new ProcessorException(message, e);
        }
        return landWaterSymbols;
    }

    private static float applyLog10(final double value) {
        if (value <= 0) {
            return 0;
        }
        return (float) (Math.log10(value));
    }

    private static MetadataElement getProcessorMetadata() {
        final MetadataElement metadata = new MetadataElement("Processor");
        metadata.addAttribute(
                new MetadataAttribute("Name", ProductData.createInstance(EutrophicLakesConstants.PROCESSOR_NAME), true));
        metadata.addAttribute(new MetadataAttribute("Version",
                                                    ProductData.createInstance(EutrophicLakesConstants.PROCESSOR_VERSION),
                                                    true));
        metadata.addAttribute(
                new MetadataAttribute("Copyright",
                                      ProductData.createInstance(EutrophicLakesConstants.PROCESSOR_COPYRIGHT_INFO), true));
        return metadata;
    }
    private static class ToaReflecSymbol extends AbstractSymbol.D {

        private double value;

        public ToaReflecSymbol(final String name) {
            super(name);
            this.value = 0;
        }

        public double evalD(EvalEnv env) throws EvalException {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }
    }

}
