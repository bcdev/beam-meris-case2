package org.esa.beam.meris.case2.algorithm;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.awt.Rectangle;

import static java.lang.Math.*;
import static org.esa.beam.dataio.envisat.EnvisatConstants.*;
import static org.esa.beam.util.math.MathUtils.*;

public class MerisFlightDirection {

    private int nadirColumnIndex;
    private double[] sunZenNadir;
    private double[] sunAziNadir;
    private double[] latNadir;
    private double[] lonNadir;
    private double lonNadir0;
    private double latNadir0;


    public MerisFlightDirection(Product merisProduct) throws IllegalArgumentException {
        nadirColumnIndex = findNadirColumnIndex(merisProduct);
        if (!merisProduct.containsPixel(nadirColumnIndex, 0)) {
            // todo (mp - 20101014) need a solution for computing the flight direction even if the nadir is
            // outside of the scene bounds
            throw new IllegalArgumentException("Product does not contain the nadir line.");
        }

        sunZenNadir = loadNadirGridColumnData(merisProduct, MERIS_SUN_ZENITH_DS_NAME, nadirColumnIndex);
        sunAziNadir = loadNadirGridColumnData(merisProduct, MERIS_SUN_AZIMUTH_DS_NAME, nadirColumnIndex);
        latNadir = loadNadirGridColumnData(merisProduct, MERIS_LAT_DS_NAME, nadirColumnIndex);
        lonNadir = loadNadirGridColumnData(merisProduct, MERIS_LON_DS_NAME, nadirColumnIndex);

        for (int i = 0; i < latNadir.length; i++) {
            latNadir[i] = Math.toRadians(latNadir[i]);
            lonNadir[i] = Math.toRadians(lonNadir[i]);
        }
        lonNadir0 = lonNadir[0];
        latNadir0 = latNadir[0];

    }

    public int getNadirColumnIndex() {
        return nadirColumnIndex;
    }

    public double getNadirSunZenith(int line) {
        return sunZenNadir[line];
    }

    public double getNadirSunAzimuth(int line) {
        return sunAziNadir[line];
    }

    public double computeMerisFlightDirection(int pixelX, double alpha) {
        return alpha + (pixelX < nadirColumnIndex ? 90.0 : 270.0);
    }

    public double computeFlightDirectionAlpha(int pixelY) {
        final double lat1 = latNadir[pixelY];
        final double lon1 = lonNadir[pixelY];

        double lonDiff = lonNadir0 - lon1;
        double cosG = sin(latNadir0) * sin(lat1) + cos(latNadir0) * cos(lat1) * cos(lonDiff);
        double g = acos(cosG);
        double sinAlpha = cos(latNadir0) * sin(lonNadir0 - lon1) / sin(g);
        return toDegrees(asin(sinAlpha));
    }

    private double[] loadNadirGridColumnData(Product merisProduct, String tpgName, int nadirColumnIndex) {
        final int rasterHeight = merisProduct.getSceneRasterHeight();
        final double[] data = new double[rasterHeight];
        final Rectangle centerColumn = new Rectangle(nadirColumnIndex, 0, 1, data.length);
        final TiePointGrid grid = merisProduct.getTiePointGrid(tpgName);
        grid.getGeophysicalImage().getData(centerColumn).getPixels(nadirColumnIndex, 0, 1, data.length, data);
        return data;
    }

    private int findNadirColumnIndex(Product merisProduct) {
        final int rasterWidth = merisProduct.getSceneRasterWidth();
        final TiePointGrid grid = merisProduct.getTiePointGrid(MERIS_VIEW_ZENITH_DS_NAME);
        final double[] data = new double[rasterWidth];
        final Rectangle centerColumn = new Rectangle(0, 0, data.length, 1);
        grid.getGeophysicalImage().getData(centerColumn).getPixels(0, 0, data.length, 1, data);
        return findNadirColumnIndex(data);
    }

    static int findNadirColumnIndex(double[] viewZenithRow) {
        double minValue = viewZenithRow[0];
        int nadirIndex = 0;
        for (int i = 1; i < viewZenithRow.length; i++) {
            if (viewZenithRow[i] < minValue) {
                minValue = viewZenithRow[i];
                nadirIndex = i;
            } else {
                break;
            }
        }

        if (nadirIndex == 0) { // we are on the left side
            final double stepSize = abs(viewZenithRow[0] - viewZenithRow[1]);
            nadirIndex -= ceilInt(minValue / stepSize);
        } else if (nadirIndex == viewZenithRow.length - 1) { // we are on the right side
            final double stepSize = abs(
                    viewZenithRow[viewZenithRow.length - 1] - viewZenithRow[viewZenithRow.length - 2]);
            nadirIndex += floorInt(minValue / stepSize);
        }
        return nadirIndex;
    }


}
