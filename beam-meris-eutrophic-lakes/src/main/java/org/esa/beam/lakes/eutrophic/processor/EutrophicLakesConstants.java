package org.esa.beam.lakes.eutrophic.processor;

import org.esa.beam.case2.processor.Case2ProcessorConstants;

public class EutrophicLakesConstants extends Case2ProcessorConstants {


    @Override
    public String getProcessorName() {
        return "MERIS Eutrophic Lakes Processor";
    }

    @Override
    public String getProcessorCopyrightInfo() {
        return "BC, GKSS, HUT, SYKE, FMI, EOMAP, CEDEX, NIVA";
    }

    @Override
    public String getProcessorHelpId() {
        return "eutrophicIntroduction";
    }

    @Override
    public String getProcessorLoggerName() {
        return "beam.processor.lakes.eutrophic";
    }

    @Override
    public String getProcessingRequestType() {
        return "EUTROPHIC_LAKES";
    }

    @Override
    public String getOutputProductType() {
        return "MER_2P_LAKES_EUT";
    }

    @Override
    public String getDefaultLogPrefix() {
        return "lakes_eutrophic";
    }

    @Override
    public String getAuxdataDirProperty() {
        return "lakes.eutrophic.auxdata.dir";
    }

    @Override
    public String getDefaultOutputFileName() {
        return "meris_lakes_eutrophic";
    }

    @Override
    public String getReadmeFileName() {
        return "aboutEutrophic.html";
    }
}
