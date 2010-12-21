package org.esa.beam.meris.case2.algorithm;

import org.esa.beam.framework.datamodel.ProductData;

public class BandDescriptor {

    private String name;
    private String unit;
    private String description;
    private int type;
    private String validExpression;
    private double scalingFactor;
    private boolean log10Scaled;
    private float spectralWavelength;
    private float spectralBandwidth;
    private int spectralBandIndex;
    private boolean writeEnabled;
    private ProductData value;
    private float initialValue;

    public BandDescriptor(String name, String description, int type, String unit, float initialValue) {
        this.description = description;
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.validExpression = "not l2_flags.LAND and not l2_flags.CLOUD_ICE";
        scalingFactor = 1.0f;
        log10Scaled = false;
        spectralWavelength = 0;
        spectralBandwidth = 0;
        spectralBandIndex = -1;
        writeEnabled = true;
        value = ProductData.createInstance(type);
        this.initialValue = initialValue;
        value.setElemDouble(initialValue);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public String getValidExpression() {
        return validExpression;
    }

    public void setValidExpression(String validExpression) {
        this.validExpression = validExpression;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public boolean isLog10Scaled() {
        return log10Scaled;
    }

    public void setLog10Scaled(boolean log10Scaled) {
        this.log10Scaled = log10Scaled;
    }

    public int getSpectralBandIndex() {
        return spectralBandIndex;
    }

    public void setSpectralBandIndex(int spectralBandIndex) {
        this.spectralBandIndex = spectralBandIndex;
    }

    public float getSpectralBandwidth() {
        return spectralBandwidth;
    }

    public void setSpectralBandwidth(float spectralBandwidth) {
        this.spectralBandwidth = spectralBandwidth;
    }

    public float getSpectralWavelength() {
        return spectralWavelength;
    }

    public void setSpectralWavelength(float spectralWavelength) {
        this.spectralWavelength = spectralWavelength;
    }

    public boolean isWriteEnabled() {
        return writeEnabled;
    }

    public void setWriteEnabled(boolean writeEnabled) {
        this.writeEnabled = writeEnabled;
    }

    public ProductData getValue() {
        return value;
    }

    public void setDoubleValue(double value) {
        this.value.setElemDouble(value);
    }

    public void setIntValue(int value) {
        this.value.setElemInt(value);
    }

    public float getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(float initialValue) {
        if (getValue().getElemDouble() == this.initialValue) {
            setDoubleValue(initialValue);
        }
        this.initialValue = initialValue;
    }

}
