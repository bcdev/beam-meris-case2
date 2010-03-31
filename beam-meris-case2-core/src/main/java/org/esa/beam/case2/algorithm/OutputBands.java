package org.esa.beam.case2.algorithm;

import org.esa.beam.framework.datamodel.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputBands {

    private Map<String, BandDescriptor> bandDescriptorMap;
    private List<BandDescriptor> bandDescriptorList;
    private Map<String, List<BandDescriptor>> prefixMap;
    private Product product;

    public OutputBands() {
        bandDescriptorMap = new HashMap<String, BandDescriptor>(50);
        bandDescriptorList = new ArrayList<BandDescriptor>(50);
        prefixMap = new HashMap<String, List<BandDescriptor>>(27);

    }

    public void setValue(String name, double value) {
        getDescriptor(name).setDoubleValue(value);
    }

    public void setValue(String name, int value) {
        getDescriptor(name).setIntValue(value);
    }

    public void setValues(String namePrefix, int[] values) {
        List<BandDescriptor> descriptors = getDescriptorsWithPrefix(namePrefix);
        for (int i = 0; i < descriptors.size(); i++) {
            descriptors.get(i).setIntValue(values[i]);
        }
    }

    public void setValues(String namePrefix, double[] values) {
        List<BandDescriptor> descriptors = getDescriptorsWithPrefix(namePrefix);
        for (int i = 0; i < values.length; i++) {
            descriptors.get(i).setDoubleValue(values[i]);
        }
    }

    public double getDoubleValue(String name) {
        return getDescriptor(name).getValue().getElemDouble();
    }

    public double[] getDoubleValues(String namePrefix) {
        List<BandDescriptor> descriptors = getDescriptorsWithPrefix(namePrefix);
        double[] values = new double[descriptors.size()];
        for (int i = 0; i < descriptors.size(); i++) {
            BandDescriptor bandDescriptor = descriptors.get(i);
            values[i] = bandDescriptor.getValue().getElemDouble();
        }

        return values;
    }

    public int getIntValue(String name) {
        return getDescriptor(name).getValue().getElemInt();
    }

    public BandDescriptor getDescriptor(String name) {
        return bandDescriptorMap.get(name);
    }

    public void addDescriptor(BandDescriptor ... bandDescriptors) {
        for (BandDescriptor bandDescriptor : bandDescriptors) {
            bandDescriptorMap.put(bandDescriptor.getName(), bandDescriptor);
        }
        bandDescriptorList.addAll(Arrays.asList(bandDescriptors));
    }

    public BandDescriptor[] getAllDescriptors() {
        return bandDescriptorList.toArray(new BandDescriptor[bandDescriptorList.size()]);
    }

    private List<BandDescriptor> getDescriptorsWithPrefix(String namePrefix) {
        List<BandDescriptor> descriptors = prefixMap.get(namePrefix);
        if(descriptors == null) {
            descriptors = new ArrayList<BandDescriptor>(bandDescriptorList.size());
            for (BandDescriptor bandDescriptor : bandDescriptorList) {
                if (bandDescriptor.getName().startsWith(namePrefix)) {
                    descriptors.add(bandDescriptor);
                }
            }
            prefixMap.put(namePrefix, descriptors);
        }
        return descriptors;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
