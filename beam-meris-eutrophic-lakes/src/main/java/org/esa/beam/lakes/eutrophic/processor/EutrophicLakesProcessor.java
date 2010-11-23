package org.esa.beam.lakes.eutrophic.processor;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.fit.MerisGLM;
import org.esa.beam.case2.algorithm.water.WaterAlgorithm;
import org.esa.beam.case2.processor.Case2Processor;
import org.esa.beam.lakes.eutrophic.algorithm.EutrophicAlgorithmParameter;
import org.esa.beam.lakes.eutrophic.algorithm.case2water.EutrophicWater;


public class EutrophicLakesProcessor extends Case2Processor {

    private WaterAlgorithm eutrophicWaterAlgo;
    private MerisGLM merisGLM;

    public EutrophicLakesProcessor() {
        super(new EutrophicLakesConstants());
        merisGLM = new MerisGLM(11, 8);
        eutrophicWaterAlgo = new EutrophicWater();
    }

    @Override
    protected Class<? extends AlgorithmParameter> getParameterType() {
        return EutrophicAlgorithmParameter.class;
    }

    @Override
    protected MerisGLM getMerisGLM() {
        return merisGLM;
    }

    @Override
    protected WaterAlgorithm getWaterAlgorithm() {
        return eutrophicWaterAlgo;
    }
}
