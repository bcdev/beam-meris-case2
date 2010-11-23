package org.esa.beam.lakes.boreal.processor;

import org.esa.beam.case2.algorithm.AlgorithmParameter;
import org.esa.beam.case2.algorithm.fit.MerisGLM;
import org.esa.beam.case2.algorithm.water.WaterAlgorithm;
import org.esa.beam.case2.processor.Case2Processor;
import org.esa.beam.lakes.boreal.algorithm.BorealAlgorithmParameter;
import org.esa.beam.lakes.boreal.algorithm.case2water.BorealWater;


public class BorealLakesProcessor extends Case2Processor {

    private MerisGLM merisGLM;
    private BorealWater borealWaterAlgo;

    public BorealLakesProcessor() {
        super(new BorealLakesConstants());
        merisGLM = new MerisGLM(10, 7);
        borealWaterAlgo = new BorealWater();
    }

    @Override
    protected Class<? extends AlgorithmParameter> getParameterType() {
        return BorealAlgorithmParameter.class;
    }

    @Override
    protected MerisGLM getMerisGLM() {
        return merisGLM;
    }

    @Override
    protected WaterAlgorithm getWaterAlgorithm() {
        return borealWaterAlgo;
    }

}
