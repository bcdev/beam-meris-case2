package org.esa.beam.meris.case2.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

/**
 * Description of RasterBlockMap
 *
 * @author Marco Peters
 */
public class RasterBlockMap {

    private HashMap<String, RasterBlock> rasterBlockMap;
    private int linesPerBlock;

    public RasterBlockMap(int linesPerBlock) {
        rasterBlockMap = new HashMap<String, RasterBlock>(12);
        this.linesPerBlock = linesPerBlock;
    }

    public void addRaster(final RasterDataNode raster, final float initValue) {
        if (raster != null) {
            rasterBlockMap.put(raster.getName(), new RasterBlock(raster, initValue, linesPerBlock));
        }
    }

    public void addRaster(final RasterDataNode raster) {
        if (raster != null) {
            rasterBlockMap.put(raster.getName(), new RasterBlock(raster, linesPerBlock));
        }
    }

    public RasterDataNode getRaster(final String rasterName) {
        if (rasterBlockMap.containsKey(rasterName)) {
            return (rasterBlockMap.get(rasterName)).raster;
        }
        return null;
    }

    public int getLinesPerBlock() {
        return linesPerBlock;
    }

    public void clear() {
        rasterBlockMap.clear();
    }

    public int readBlock(final int startIndex) throws IOException {
        final Collection<RasterBlock> rasterLines = rasterBlockMap.values();
        int linesRead = 0;
        for (RasterBlock rasterBlock : rasterLines) {
            linesRead = rasterBlock.readBlock(startIndex);
        }
        return linesRead;
    }

    public void writeBlock(final int startIndex) throws IOException {
        final Collection<RasterBlock> rasterBlocks = rasterBlockMap.values();
        for (RasterBlock rasterBlock : rasterBlocks) {
            rasterBlock.writeBlock(startIndex);
        }
    }

    public float getPixelFloat(final String rasterName, final int pixelIndex) {
        final RasterBlock rasterBlock = getRasterBlock(rasterName);
        return rasterBlock.blockData.getElemFloatAt(pixelIndex);
    }

    public int getPixelInt(final String rasterName, final int pixelIndex) {
        final RasterBlock rasterBlock = getRasterBlock(rasterName);
        return rasterBlock.blockData.getElemIntAt(pixelIndex);
    }

    public void setPixel(final String rasterName, final int pixelIndex, final double value) {
        if (rasterBlockMap.containsKey(rasterName)) {
            final RasterBlock rasterBlock = getRasterBlock(rasterName);
            rasterBlock.blockData.setElemDoubleAt(pixelIndex, value);
        }
    }

    public void setPixel(final String rasterName, final int pixelIndex, final int value) {
        if (rasterBlockMap.containsKey(rasterName)) {
            final RasterBlock rasterBlock = getRasterBlock(rasterName);
            rasterBlock.blockData.setElemIntAt(pixelIndex, value);
        }
    }

    private RasterBlock getRasterBlock(final String rasterName) {
        if (!rasterBlockMap.containsKey(rasterName)) {
            throw new IllegalArgumentException("the raster name '" + rasterName + "' is unknown");
        }
        return rasterBlockMap.get(rasterName);
    }


    private static class RasterBlock {

        private final RasterDataNode raster;
        public ProductData blockData;
        private int numLines;
        private float initialValue;

        public RasterBlock(final RasterDataNode raster) {
            this(raster, 1);
        }

        public RasterBlock(final RasterDataNode raster, int numLines) {
            this.raster = raster;
            this.numLines = numLines;
            blockData = createBlockData(raster, numLines);
        }

        private ProductData createBlockData(RasterDataNode raster, int numLines) {
            if (raster.isFloatingPointType()) {
                return ProductData.createInstance(ProductData.TYPE_FLOAT32, raster.getSceneRasterWidth() * numLines);
            } else {
                return ProductData.createInstance(ProductData.TYPE_INT32, raster.getSceneRasterWidth() * numLines);
            }
        }

        public RasterBlock(final RasterDataNode raster, final float initialValue) {
            this(raster, initialValue, 1);
        }

        public RasterBlock(final RasterDataNode raster, final float initialValue, int numLines) {
            this(raster, numLines);
            this.initialValue = initialValue;
            initializeBlockData();
        }

        private void initializeBlockData() {
            if (raster.isFloatingPointType()) {
                final float[] data = (float[]) blockData.getElems();
                Arrays.fill(data, initialValue);
            } else {
                final int[] data = (int[]) blockData.getElems();
                Arrays.fill(data, (int) initialValue);
            }
        }

        public int readBlock(final int startIndex) throws IOException {
            return readSceneRasterBlock(startIndex);
        }

        public void writeBlock(final int startIndex) throws IOException {
            int linesRemaining = raster.getSceneRasterHeight() - startIndex;
            int linesToWrite = Math.min(numLines, linesRemaining);
            if (linesToWrite < numLines) {
                ProductData newBlock = createBlockData(raster, linesToWrite);
                System.arraycopy(blockData.getElems(), 0, newBlock.getElems(), 0, newBlock.getNumElems());
                blockData = newBlock;
            }
            raster.writeRasterData(0, startIndex,
                                   raster.getSceneRasterWidth(), linesToWrite,
                                   blockData, ProgressMonitor.NULL);
        }

        private int readSceneRasterBlock(final int startIndex) throws IOException {
            int linesRemaining = raster.getSceneRasterHeight() - startIndex;
            int linesToRead = Math.min(numLines, linesRemaining);
            if (linesToRead < numLines) {
                createBlockData(raster, linesToRead);
                initializeBlockData();
            }
            if (raster.isFloatingPointType()) {
                raster.readPixels(0, startIndex, raster.getSceneRasterWidth(), linesToRead,
                                  (float[]) blockData.getElems(), ProgressMonitor.NULL);
            } else {
                raster.readPixels(0, startIndex, raster.getSceneRasterWidth(), linesToRead,
                                  (int[]) blockData.getElems(), ProgressMonitor.NULL);
            }
            return linesToRead;
        }
    }
}
