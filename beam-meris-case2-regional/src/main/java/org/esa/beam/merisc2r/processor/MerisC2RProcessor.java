package org.esa.beam.merisc2r.processor;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.fit.MerisGLM;
import org.esa.beam.case2.algorithm.water.WaterAlgorithm;
import org.esa.beam.case2.processor.Case2Processor;
import org.esa.beam.merisc2r.algorithm.RegionalAlgorithmParameter;
import org.esa.beam.merisc2r.algorithm.case2water.RegionalWater;


public class MerisC2RProcessor extends Case2Processor {

    private MerisGLM merisGLM;
    private RegionalWater regionalWaterAlgo;

    public MerisC2RProcessor() {
        super(new MerisC2RConstants());
        merisGLM = new MerisGLM(11, 8);
        regionalWaterAlgo = new RegionalWater();
    }

    @Override
    protected Class<? extends AlgorithmParameter> getParameterType() {
        return RegionalAlgorithmParameter.class;
    }

    @Override
    protected MerisGLM getMerisGLM() {
        return merisGLM;
    }

    @Override
    protected WaterAlgorithm getWaterAlgorithm() {
        return regionalWaterAlgo;
    }

}
