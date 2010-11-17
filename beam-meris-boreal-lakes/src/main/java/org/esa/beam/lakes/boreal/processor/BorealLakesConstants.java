package org.esa.beam.lakes.boreal.processor;

import org.esa.beam.case2.processor.Case2ProcessorConstants;

public class BorealLakesConstants extends Case2ProcessorConstants {

    @Override
    public String getProcessorName() {
        return "MERIS Boreal Lakes Processor";
    }

    @Override
    public String getProcessorCopyrightInfo() {
        return "BC, GKSS, HUT, SYKE, FMI, EOMAP, CEDEX, NIVA";
    }

    @Override
    public String getProcessorHelpId() {
        return "borealIntroduction";
    }

    @Override
    public String getProcessorLoggerName() {
        return "beam.processor.lakes.boreal";
    }

    @Override
    public String getProcessingRequestType() {
        return "BOREAL_LAKES";
    }

    @Override
    public String getOutputProductType() {
        return "MER_2P_LAKES_BOR";
    }

    @Override
    public String getDefaultLogPrefix() {
        return "lakes_boreal";
    }

    @Override
    public String getAuxdataDirProperty() {
        return "lakes.boreal.auxdata.dir";
    }

    @Override
    public String getDefaultOutputFileName() {
        return "meris_lakes_boreal";
    }

    @Override
    public String getReadmeFileName() {
        return "aboutBoreal.html";
    }
}
