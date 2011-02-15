package org.esa.beam.meris.case2.util.nn;

import org.esa.beam.nn.NNffbpAlphaTabFast;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author schiller
 */
public class InvNNReflCut extends NNffbpAlphaTabFast {

    double reflCut;
    public int count;

    public InvNNReflCut(InputStream neuralNetStream, double reflCut) throws IOException {
        super(neuralNetStream);
        this.reflCut = reflCut;
    }

    @Override
    public double[] calc(double[] nninp) {
        this.count = 0;
        for (int i = 0; i < 8; i++) {
            if (nninp[3 + i] < this.reflCut) {
                nninp[3 + i] = this.reflCut;
                this.count++;
            }
        }
        return super.calc(nninp);
    }

}
