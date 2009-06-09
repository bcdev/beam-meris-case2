package org.esa.beam.case2.algorithm;

import com.bc.jexp.Term;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.2 $ $Date: 2007-07-12 15:39:22 $
 */
public class PixelData {

    public double[] toa_radiance = new double[15];     /* toa radiance in W m-2 sr-1 µm-1 */
    public double[] toa_reflectance = new double[15];         /* toa radiance reflectance in sr-1 */
    public double[] solar_flux = new double[15];     /* at toa W m-2 µm-1, incl. sun-earth distance */
    /* pixel position in image(1-based)*/
    public int column;
    public int row;
    public float lat;
    public float lon;
    public float altitude;
    public double solzen;        /* Solar zenith angle in deg [0,90].........*/
    public double solazi;        /* Solar azimuth angle in deg [0-360I]		*/
    public double satzen;        /* Satellite zenith angle in deg [0,90]		*/
    public double satazi;        /* Satellite azimuth angle as viewed from pixel in deg [0-360I]	*/

    public double pressure;    /* Surface pressure in hPa	    	   	*/
    public double ozone;              /* Total ozone concentration in DU		*/
    public double windspeed;    /* Surface windspeed in m/s	       		*/
    public int detectorIndex;   /* detector Index */
    public boolean isFullResolution;

    public Term landWaterTerm;
    public Term cloudIceTerm;
}
