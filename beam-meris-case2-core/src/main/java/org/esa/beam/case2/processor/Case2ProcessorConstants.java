package org.esa.beam.case2.processor;

import org.esa.beam.framework.processor.ProcessorConstants;

public abstract class Case2ProcessorConstants implements ProcessorConstants {

    public abstract String getProcessorName();

    public abstract String getProcessorCopyrightInfo();

    public abstract String getProcessorHelpId();

    public abstract String getProcessorLoggerName();

    public abstract String getProcessingRequestType();

    public abstract String getOutputProductType();

    public abstract String getDefaultLogPrefix();

    public abstract String getAuxdataDirProperty();


    public abstract String getDefaultOutputFileName();

    public abstract String getReadmeFileName();

    public String getDefaultParameterFileName() {
        return "default-parameters.txt";
    }

    public String getProcessorVersion() {
        return "1.4.3";
    }

    /*
    * Name of the parameter which holds the processing parameter file path
    */
    public String getPropertyFileParamName() {
        return "property_file";
    }

    public Boolean getDefaultLogToOutput() {
        return Boolean.FALSE;
    }

}
