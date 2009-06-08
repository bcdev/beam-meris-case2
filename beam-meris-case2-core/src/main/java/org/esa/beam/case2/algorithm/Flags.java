package org.esa.beam.case2.algorithm;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.1 $ $Date: 2007-06-05 11:32:00 $
 */
public interface Flags {

    //l2_flags attributes
    public static final int RAD_ERR = 0x1;          /* TOAR toa radiance reflectance out of valid range */
    public static final int LAND = 0x2;             /* land pixel				        */
    public static final int CLOUD_ICE = 0x4;        /* cloud or ice				        */
    public static final int SUNGLINT = 0x8;            /* sunglint risk		 	        */      
    public static final int ANCIL = 0x10;            /* missing/OOR auxiliary data 	    */
    public static final int TOSA_OOR = 0x20;            /* TOAR out of scope			    */
    public static final int WLR_OOR = 0x40;            /* WLR out of scope			        */
    public static final int SOLZEN = 0x80;            /* large solar zenith angle		    */
    public static final int SATZEN = 0x100;            /* large spacecraft zenith angle    */       // todo - never set
    public static final int ATC_OOR = 0x200;            /* atmospheric correction out of range */
    public static final int CONC_OOR = 0x400;           /* concentration out of range */
    public static final int OOTR = 0x800;               /* out of training range == chi2 of measured and fwNN spectrum above threshold*/
    public static final int WHITECAPS = 0x1000;         // risk for white caps
    public static final int FIT_FAILED = 0x2000;        // Chi square fit passed threshold
    public static final int SPAREFLAG06 = 0x4000;
    public static final int SPAREFLAG07 = 0x8000;
    public static final int INVALID = 0x10000;      // not a usable water pixel

    // QUALITY FLAGS
    //   SPECTRUM_OOS       - Reflectance spectrum out-of-scope
    //   UNCERTAIN_ATM_CORR - Uncertain water leaving radiance reflectance
    //   CLOUD              - Pixel shows clouds
    //   WATER              - Pixel shows water

    
}
