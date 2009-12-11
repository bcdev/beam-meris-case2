package org.esa.beam.lakes.boreal.processor;

import org.esa.beam.framework.processor.ProcessorConstants;

public interface BorealLakesConstants extends ProcessorConstants {

    String PROCESSOR_NAME = "MERIS Boreal Lakes Processor";
    String PROCESSOR_VERSION = "1.4";
    String PROCESSOR_COPYRIGHT_INFO = "BC, GKSS, HUT, SYKE, FMI, EOMAP, CEDEX, NIVA";
    String PROCESSOR_LOGGER_NAME = "beam.processor.lakes.boreal";
    String PROCESSOR_HELP_ID = "borealIntroduction";
    String PROCESSING_REQUEST_TYPE = "BOREAL_LAKES";
    String OUTPUT_PRODUCT_TYPE = "MER_2P_LAKES_BOR";
    String OUTPUT_FORMAT = "BEAM-DIMAP";
    String DEFAULT_LOG_PREFIX = "lakes_boreal";
    String AUXDATA_DIR_PROPERTY= "lakes.boreal.auxdata.dir";
    String DEFAULT_PARAMETER_FILE_NAME = "default-parameters.txt";
    /**
     * Name of the parameter which holds the processing parameter file path
     */
    String PROPERTY_FILE_PARAM_NAME = "property_file";
    String DEFAULT_OUPUT_FILE_NAME = "meris_lakes_boreal";
    Boolean DEFAULT_LOG_TO_OUTPUT = Boolean.FALSE;
    String README_FILE_NAME = "aboutBoreal.html";
}
