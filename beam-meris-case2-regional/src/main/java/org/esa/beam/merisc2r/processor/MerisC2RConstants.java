package org.esa.beam.merisc2r.processor;

import org.esa.beam.case2.processor.Case2ProcessorConstants;

public class MerisC2RConstants extends Case2ProcessorConstants {

    @Override
    public String getProcessorName() {
        return "MERIS Case 2 Regional Processor";
    }

    @Override
    public String getProcessorCopyrightInfo() {
        return "Copyright (C) 2005 by KOF";
    }

    @Override
    public String getProcessorHelpId() {
        return "c2rIntroduction";
    }

    @Override
    public String getProcessorLoggerName() {
        return "beam.processor.merisc2r";
    }

    @Override
    public String getProcessingRequestType() {
        return "MERISC2R";
    }

    @Override
    public String getOutputProductType() {
        return "MER_2P_C2R";
    }

    @Override
    public String getDefaultLogPrefix() {
        return "merisc2r";
    }

    @Override
    public String getAuxdataDirProperty() {
        return "merisc2r.auxdata.dir";
    }

    @Override
    public String getDefaultOutputFileName() {
        return "merisc2r";
    }

    @Override
    public String getReadmeFileName() {
        return "aboutC2R.html";
    }
}
