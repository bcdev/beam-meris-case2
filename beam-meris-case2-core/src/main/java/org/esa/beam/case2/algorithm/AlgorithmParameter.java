package org.esa.beam.case2.algorithm;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class AlgorithmParameter {

    public String waterNnInverseFilePath = "";
    public String waterNnForwardFilePath = "";
    public String atmCorrNnFilePath = "./atmo_net_20091105/25x30x35x40_11415.7.net";
    public String polCorrNnFilePath = "./18_518.1.netPolEffekt";
    public String smileAuxdataDirPath = "./smile";
    public String inputValidMask = "not l1_flags.INVALID and not l1_flags.SUSPECT and not l1_flags.BRIGHT";

    public String landWaterSeparationExpression = "toa_reflec_10 > toa_reflec_6 AND toa_reflec_13 > 0.0475";
    public String cloudIceDetectionExpression = "toa_reflec_14 > 0.2";

    public boolean performSmileCorrection = true;
    public boolean performAtmosphericCorrection = true;
    public boolean performChiSquareFit = false;

    public double fitFailedThreshold = 14.0;
    public double waterReflLogVariance = 1.5; // Variance of Log of Water Reflectance
    public boolean useInvNN = false;
    public double fitCut = -10; // cut value used in LvMq routine
    public int nIterMax = 30;
    public double nu = 2.0;
    public double tau = 0.05;
    public double eps1 = 0.01;
    public double eps2 = 0.0003;
    public boolean performPolCorr = true;
    public double tsmConversionFactor = 1.73;
    public double tsmConversionExponent = 1.0;
    public double chlConversionFactor = 21.0;
    public double chlConversionExponent = 1.04;
    
    public double radiance1AdjustmentFactor = 1.0;         /* adjustment of TOA reflectance MERIS band 1 (412 nm)*/
    public double spectrumOutOfScopeThreshold = 4.0;
    public boolean outputPathRadianceReflAll = false;
    public final boolean[] outputPathRadianceRefl = new boolean[]{true, true, true, true, true, true, true, true, true, true, false, true, true}; // 12, band 11 is skiped
    public boolean switchToIrradianceReflectance = false;
    public boolean outputWaterLeavingReflAll = false;
    public final boolean[] outputWaterLeavingRefl = new boolean[]{true, true, true, true, true, true, true, true, true, true, false, true, true}; // 12, band 11 is skiped
    public boolean outputTransmittanceAll = false;
    public final boolean[] outputTransmittance = new boolean[]{true, true, true, true, true, true, true, true, true, true, false, true, true}; // 12, band 11 is skiped
    public boolean outputToaReflAll = false;
    public final boolean[] outputToaRefl = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true, true, true, true}; // 15
    public boolean outputTosaReflAll = false;
    public final boolean[] outputTosaRefl = new boolean[]{true, true, true, true, true, true, true, true, true, true, false, true, true, false, false}; // 15
    public boolean outputRlToa13 = false;
    public boolean outputAPig = true; // "a_pig"
    public boolean outputAGelb = true; // "a_gelb"
    public boolean outputBTsm = true; // "b_tsm"
    public boolean outputChlConc = true; // "chl_conc"
    public boolean outputTsmConc = true; // "tsm_conc"
    public boolean outputAngstrom = false;
    public boolean outputTau = false;
    public boolean outputFitBTsm = true;
    public boolean outputFitAPig = true;
    public boolean outputFitAGelb = true;
    public boolean outputFitTsmConc = true;
    public boolean outputFitChlConc = true;
    public boolean outputChiSquareFit = true;
    public boolean outputNIter = true;
    public boolean outputParamChange = false;
    public boolean outputKmin = true;
    public boolean outputZ90max = true;
    public boolean outputATotal = true;
    public boolean outputOutOfScopeChiSquare = true; // "oos_chi2"
}
