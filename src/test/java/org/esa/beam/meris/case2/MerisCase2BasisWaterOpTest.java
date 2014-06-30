package org.esa.beam.meris.case2;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

public class MerisCase2BasisWaterOpTest {

    @Test
    public void testGetAzimuthDifference() throws Exception {
        assertEquals(172.0, MerisCase2BasisWaterOp.getAzimuthDifference(92.0, 100.0), 1.0e-8);
        assertEquals(172.0, MerisCase2BasisWaterOp.getAzimuthDifference(100.0, 92.0), 1.0e-8);
        assertEquals(10.0, MerisCase2BasisWaterOp.getAzimuthDifference(90.0, 280.0), 1.0e-8);
        assertEquals(10.0, MerisCase2BasisWaterOp.getAzimuthDifference(280.0, 90.0), 1.0e-8);
    }

    @Test
    public void testIsProductMerisFullResolution_FromGlobalAttributes() throws Exception {
        Product product = new Product("dummy", "type", 2, 2);
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        product.getMetadataRoot().addElement(globalAttributes);

        MetadataAttribute productTypeAttribute = new MetadataAttribute("product_type", ProductData.createInstance("MER_RRG_L1P"), false);
        globalAttributes.addAttribute(productTypeAttribute);
        assertFalse(MerisCase2BasisWaterOp.isFullResolution(product));

        globalAttributes.removeAttribute(productTypeAttribute);

        productTypeAttribute = new MetadataAttribute("product_type", ProductData.createInstance("MER_FSG"), false);
        globalAttributes.addAttribute(productTypeAttribute);
        assertTrue(MerisCase2BasisWaterOp.isFullResolution(product));
    }

    @Test
    public void testIsProductMerisFullResolution_FromProductType() throws Exception {
        Product product = new Product("dummy", "type", 2, 2);
        product.setProductType("MER_RRG_L1P");
        assertFalse(MerisCase2BasisWaterOp.isFullResolution(product));

        product.setProductType("MER_FSG");
        assertTrue(MerisCase2BasisWaterOp.isFullResolution(product));
    }

    @Test
    public void testIsProductMerisFullResolution_FromSPH() throws Exception {
        Product product = new Product("dummy", "type", 2, 2);
        MetadataElement sphElement = new MetadataElement("SPH");
        product.getMetadataRoot().addElement(sphElement);

        MetadataAttribute sphDescriptorAttribute = new MetadataAttribute("SPH_DESCRIPTOR", ProductData.createInstance("MER_RR__1P SPECIFIC HEADER"),
                                                                         false);
        sphElement.addAttribute(sphDescriptorAttribute);
        assertFalse(MerisCase2BasisWaterOp.isFullResolution(product));

        sphElement.removeAttribute(sphDescriptorAttribute);

        sphDescriptorAttribute = new MetadataAttribute("SPH_DESCRIPTOR", ProductData.createInstance("MER_FSG_1P SPECIFIC HEADER"), false);
        sphElement.addAttribute(sphDescriptorAttribute);
        assertTrue(MerisCase2BasisWaterOp.isFullResolution(product));

    }

}
