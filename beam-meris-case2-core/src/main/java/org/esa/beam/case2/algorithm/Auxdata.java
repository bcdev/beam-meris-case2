package org.esa.beam.case2.algorithm;

import org.esa.beam.case2.util.nn.NNffbpAlphaTabFast;
import org.esa.beam.case2.algorithm.fit.ErrorFit3_v2;
import org.esa.beam.processor.smile.SmileAuxData;


/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.2 $ $Date: 2007-07-12 12:10:43 $
 */
public class Auxdata {

    private NNffbpAlphaTabFast waterNet;
    private NNffbpAlphaTabFast forwardWaterNet;
    private NNffbpAlphaTabFast atmosphericNet;
    private NNffbpAlphaTabFast polarizationNet;
    private ErrorFit3_v2 fitLvMq;
    private SmileAuxData smileAuxdata;

    public Auxdata(NNffbpAlphaTabFast inverseWaterNet, NNffbpAlphaTabFast forwardWaterNet,
                   NNffbpAlphaTabFast atmosphericNet, NNffbpAlphaTabFast polarizationNet,
                   SmileAuxData smileAuxdata,
                   ErrorFit3_v2 errorFit3) {
        waterNet = inverseWaterNet;
        this.forwardWaterNet = forwardWaterNet;
        this.atmosphericNet = atmosphericNet;
        this.polarizationNet = polarizationNet;
        fitLvMq = errorFit3;
        this.smileAuxdata = smileAuxdata;
    }

    public NNffbpAlphaTabFast getWaterNet() {
        return waterNet;
    }

    public NNffbpAlphaTabFast getForwardWaterNet() {
        return forwardWaterNet;
    }

    public NNffbpAlphaTabFast getAtmosphericNet() {
        return atmosphericNet;
    }


    public ErrorFit3_v2 getFitLvMq() {
        return fitLvMq;
    }

    public NNffbpAlphaTabFast getPolarizationNet() {
        return polarizationNet;
    }

    public SmileAuxData getSmileAuxdata() {
        return smileAuxdata;
    }

}
