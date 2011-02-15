package org.esa.beam.meris.case2.util.nn;

import org.esa.beam.nn.NNCalc;
import org.esa.beam.nn.NNffbpAlphaTabFast;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author schiller
 */
public class ForwNNReflCut extends NNffbpAlphaTabFast {

    private double reflCut;

    public ForwNNReflCut(InputStream neuralNetStream, double reflCut) throws IOException {
        super(neuralNetStream);
        this.reflCut = reflCut;
    }

    @Override
    public double[] calc(double[] nninp) {
        double[] nnout = super.calc(nninp);
        for (int i = 0; i < nnout.length; i++) {
            if (nnout[i] < this.reflCut) {
                nnout[i] = this.reflCut;
            }
        }
        return nnout;
    }

    @Override
    public NNCalc calcJacobi(double[] nnInp) {
        NNCalc nnCalc = super.calcJacobi(nnInp);
        final double[] nnOutput = nnCalc.getNnOutput();
        for (int i = 0; i < nnOutput.length; i++) {
            if (nnOutput[i] < this.reflCut) {
                nnOutput[i] = this.reflCut;
            }
        }
        return nnCalc;
    }
}
