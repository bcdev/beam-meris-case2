package org.esa.beam.merisc2r.processor;

import org.esa.beam.framework.processor.ProcessorConstants;

import java.io.File;

public interface MerisC2RConstants extends ProcessorConstants {

    String PROCESSOR_NAME = "MERIS Case 2 Regional Processor";
    String PROCESSOR_VERSION = "1.3.2";
    String PROCESSOR_COPYRIGHT_INFO = "Copyright (C) 2005 by KOF";
    String PROCESSOR_HELP_ID = "c2rIntroduction";
    String PROCESSOR_LOGGER_NAME = "beam.processor.merisc2r";
    String PROCESSING_REQUEST_TYPE = "MERISC2R";
    String OUTPUT_PRODUCT_TYPE = "MER_2P_C2R";
    String OUTPUT_FORMAT = "BEAM-DIMAP";
    String DEFAULT_LOG_PREFIX = "merisc2r";
    String AUXDATA_DIRNAME = "merisc2r";
    String AUXDATA_DIR_PROPERTY = "merisc2r.auxdata.dir";
    File DEFAULT_AUXDATA_DIR = new File("auxdata", AUXDATA_DIRNAME);
    String DEFAULT_PARAMETER_FILE_NAME = "default-parameters.txt";
    /**
     * Name of the parameter which holds the processing parameter file path
     */
    String PROPERTY_FILE_PARAM_NAME = "property_file";
    String DEFAULT_OUPUT_FILE_NAME = "merisc2r";
    Boolean DEFAULT_LOG_TO_OUTPUT = Boolean.FALSE;
    String README_FILE_NAME = "aboutC2R.html";
}
