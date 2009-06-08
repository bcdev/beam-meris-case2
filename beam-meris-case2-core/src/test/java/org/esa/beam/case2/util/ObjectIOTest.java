package org.esa.beam.case2.util;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.MetadataElement;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class ObjectIOTest extends TestCase {

    public void testSomething() {
        final TestDerivedObjectClass testObject = new TestDerivedObjectClass();
        final TestObjectClass castedTestObject = (TestObjectClass) testObject;
        assertEquals("Sansibar", testObject.aString);
        assertEquals("Sansibar", castedTestObject.aString);

        testObject.aString = "Bibo";
        assertEquals("Bibo", testObject.aString);
        assertEquals("Bibo", castedTestObject.aString);

        castedTestObject.aString = "Samson";
        assertEquals("Samson", testObject.aString);
        assertEquals("Samson", castedTestObject.aString);
    }

    public void testReadObjectWithEmptyInputStream() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TestObjectClass actualObject = ObjectIO.readObject(TestObjectClass.class, inputStream);
        assertNotNull(actualObject);

        final TestObjectClass expectedObject = new TestObjectClass();

        assertEquals(expectedObject.bArray[0], actualObject.bArray[0]);
        assertEquals(expectedObject.bArray[1], actualObject.bArray[1]);
        assertEquals(expectedObject.bArray[2], actualObject.bArray[2]);
        assertEquals(expectedObject.aString, actualObject.aString);
        assertEquals(expectedObject.aFloat, actualObject.aFloat, 1e-6);
        assertEquals(expectedObject.aInt, actualObject.aInt);
        assertEquals(expectedObject.cArrayAll, actualObject.cArrayAll);
        assertEquals(expectedObject.cArray[0], actualObject.cArray[0]);
        assertEquals(expectedObject.cArray[1], actualObject.cArray[1]);
        assertEquals(expectedObject.cArray[2], actualObject.cArray[2]);
        assertEquals(expectedObject.cArray[3], actualObject.cArray[3]);
    }

    public void testReadObjectFromFile() throws Exception {
        URL resource = getClass().getResource("TestObjectClass.properties");
        File testFile = new File(resource.toURI());
        final TestObjectClass actualObject = ObjectIO.readObject(TestObjectClass.class, testFile.getCanonicalPath());
        assertNotNull(actualObject);

        assertTrue(actualObject.bArray[0]);
        assertFalse(actualObject.bArray[1]);
        assertTrue(actualObject.bArray[2]);
        assertEquals("Aurora", actualObject.aString);
        assertEquals(12.34f, actualObject.aFloat, 1e-6);
        assertEquals(678, actualObject.aInt);
        assertTrue(actualObject.cArrayAll);
        assertFalse(actualObject.cArray[0]);
        assertFalse(actualObject.cArray[1]);
        assertTrue(actualObject.cArray[2]);
        assertTrue(actualObject.cArray[3]);
    }

    public void testReadDerivedObjectFromFile() throws Exception {
        URL resource = getClass().getResource("TestDerivedObjectClass.properties");
        File testFile = new File(resource.toURI());
        final TestDerivedObjectClass actualObject = ObjectIO.readObject(TestDerivedObjectClass.class, testFile.getCanonicalPath());
        assertNotNull(actualObject);

        assertFalse(actualObject.bArrayDerived[0]);
        assertFalse(actualObject.bArrayDerived[1]);
        assertTrue(actualObject.bArrayDerived[2]);
        assertEquals("Capri", actualObject.aStringDerived);
        assertEquals(99.6f, actualObject.aFloatDerived, 1e-6);
        assertEquals(123, actualObject.aIntDerived);
        assertTrue(actualObject.cArrayDerivedAll);
        assertTrue(actualObject.cArrayDerived[0]);
        assertTrue(actualObject.cArrayDerived[1]);
        assertTrue(actualObject.cArrayDerived[2]);
        assertTrue(actualObject.cArrayDerived[3]);

        assertTrue(actualObject.bArray[0]);
        assertFalse(actualObject.bArray[1]);
        assertTrue(actualObject.bArray[2]);
        assertEquals("Aurora", actualObject.aString);
        assertEquals(12.34f, actualObject.aFloat, 1e-6);
        assertEquals(678, actualObject.aInt);
        assertTrue(actualObject.cArrayAll);
        assertFalse(actualObject.cArray[0]);
        assertFalse(actualObject.cArray[1]);
        assertTrue(actualObject.cArray[2]);
        assertTrue(actualObject.cArray[3]);
    }

    public void testSetObjectProperties() throws IOException {
        final Properties objProps = new Properties();

        objProps.setProperty("bArray.1", String.valueOf(true));
        objProps.setProperty("bArray.2", String.valueOf(true));
        objProps.setProperty("bArray.3", String.valueOf(false));
        objProps.setProperty("aString", "someText");
        objProps.setProperty("aFloat", String.valueOf(12.6f));
        objProps.setProperty("aInt", String.valueOf(123456));
        objProps.setProperty("NotContained", "strange");
        objProps.setProperty("cArrayAll", String.valueOf(false));
        objProps.setProperty("cArray.1", String.valueOf(false));
        objProps.setProperty("cArray.2", String.valueOf(true));
        objProps.setProperty("cArray.3", String.valueOf(false));
        objProps.setProperty("cArray.4", String.valueOf(false));


        final TestObjectClass objectClass = new TestObjectClass();
        ObjectIO.setObjectProperties(objectClass, objProps);

        assertTrue(objectClass.bArray[0]);
        assertTrue(objectClass.bArray[1]);
        assertFalse(objectClass.bArray[2]);
        assertEquals("someText", objectClass.aString);
        assertEquals(12.6f, objectClass.aFloat, 1e-6);
        assertEquals(123456, objectClass.aInt);
        assertFalse(objectClass.cArrayAll);
        assertFalse(objectClass.cArray[0]);
        assertTrue(objectClass.cArray[1]);
        assertFalse(objectClass.cArray[2]);
        assertFalse(objectClass.cArray[3]);
    }

    public void testSetDerivedObjectProperties() throws IOException {
        final Properties objProps = new Properties();

        objProps.setProperty("bArray.1", String.valueOf(true));
        objProps.setProperty("bArray.2", String.valueOf(true));
        objProps.setProperty("bArray.3", String.valueOf(false));
        objProps.setProperty("aString", "someText");
        objProps.setProperty("aFloat", String.valueOf(12.6f));
        objProps.setProperty("aInt", String.valueOf(123456));
        objProps.setProperty("NotContained", "strange");
        objProps.setProperty("cArrayAll", String.valueOf(false));
        objProps.setProperty("cArray.1", String.valueOf(false));
        objProps.setProperty("cArray.2", String.valueOf(true));
        objProps.setProperty("cArray.3", String.valueOf(false));
        objProps.setProperty("cArray.4", String.valueOf(false));

        objProps.setProperty("bArrayDerived.1", String.valueOf(false));
        objProps.setProperty("bArrayDerived.2", String.valueOf(true));
        objProps.setProperty("bArrayDerived.3", String.valueOf(false));
        objProps.setProperty("aStringDerived", "Lyrics");
        objProps.setProperty("aFloatDerived", String.valueOf(3.80f));
        objProps.setProperty("aIntDerived", String.valueOf(-456));
        objProps.setProperty("NotContained", "strange");
        objProps.setProperty("cArrayDerivedAll", String.valueOf(false));
        objProps.setProperty("cArrayDerived.1", String.valueOf(false));
        objProps.setProperty("cArrayDerived.2", String.valueOf(true));
        objProps.setProperty("cArrayDerived.3", String.valueOf(false));
        objProps.setProperty("cArrayDerived.4", String.valueOf(true));


        final TestDerivedObjectClass objectClass = new TestDerivedObjectClass();
        ObjectIO.setObjectProperties(objectClass, objProps);

        assertFalse(objectClass.bArrayDerived[0]);
        assertTrue(objectClass.bArrayDerived[1]);
        assertFalse(objectClass.bArrayDerived[2]);
        assertEquals("Lyrics", objectClass.aStringDerived);
        assertEquals(3.8f, objectClass.aFloatDerived, 1e-6);
        assertEquals(-456, objectClass.aIntDerived);
        assertFalse(objectClass.cArrayDerivedAll);
        assertFalse(objectClass.cArrayDerived[0]);
        assertTrue(objectClass.cArrayDerived[1]);
        assertFalse(objectClass.cArrayDerived[2]);
        assertTrue(objectClass.cArrayDerived[3]);

        assertTrue(objectClass.bArray[0]);
        assertTrue(objectClass.bArray[1]);
        assertFalse(objectClass.bArray[2]);
        assertEquals("someText", objectClass.aString);
        assertEquals(12.6f, objectClass.aFloat, 1e-6);
        assertEquals(123456, objectClass.aInt);
        assertFalse(objectClass.cArrayAll);
        assertFalse(objectClass.cArray[0]);
        assertTrue(objectClass.cArray[1]);
        assertFalse(objectClass.cArray[2]);
        assertFalse(objectClass.cArray[3]);
    }

    public void testGetObjectProperties() throws IOException {
        final TestObjectClass objectClass = new TestObjectClass();
        final Properties objProps = ObjectIO.getObjectProperties(objectClass);

        assertNotNull(objProps);
        assertEquals(11, objProps.size());
        assertTrue(objProps.containsKey("bArray.1"));
        assertTrue(objProps.containsKey("bArray.2"));
        assertTrue(objProps.containsKey("bArray.3"));
        assertTrue(objProps.containsKey("aString"));
        assertTrue(objProps.containsKey("aFloat"));
        assertTrue(objProps.containsKey("aInt"));
        assertTrue(objProps.containsKey("cArrayAll"));
        assertTrue(objProps.containsKey("cArray.1"));
        assertTrue(objProps.containsKey("cArray.2"));
        assertTrue(objProps.containsKey("cArray.3"));
        assertTrue(objProps.containsKey("cArray.4"));

        assertFalse(objProps.containsKey("NotContained"));

        assertEquals(String.valueOf(objectClass.bArray[0]), (String) objProps.get("bArray.1"));
        assertEquals(String.valueOf(objectClass.bArray[1]), (String) objProps.get("bArray.2"));
        assertEquals(String.valueOf(objectClass.bArray[2]), (String) objProps.get("bArray.3"));
        assertEquals(objectClass.aString, (String) objProps.get("aString"));
        assertEquals(String.valueOf(objectClass.aFloat), (String) objProps.get("aFloat"));
        assertEquals(String.valueOf(objectClass.aInt), (String) objProps.get("aInt"));
        assertEquals(String.valueOf(objectClass.cArrayAll), (String) objProps.get("cArrayAll"));
        assertEquals(String.valueOf(objectClass.cArray[0]), (String) objProps.get("cArray.1"));
        assertEquals(String.valueOf(objectClass.cArray[1]), (String) objProps.get("cArray.2"));
        assertEquals(String.valueOf(objectClass.cArray[2]), (String) objProps.get("cArray.3"));
        assertEquals(String.valueOf(objectClass.cArray[3]), (String) objProps.get("cArray.4"));
    }

    public void testGetDerivedObjectProperties() throws IOException {
        final TestDerivedObjectClass objectClass = new TestDerivedObjectClass();
        final Properties objProps = ObjectIO.getObjectProperties(objectClass);

        assertNotNull(objProps);
        assertEquals(22, objProps.size());
        assertTrue(objProps.containsKey("bArrayDerived.1"));
        assertTrue(objProps.containsKey("bArrayDerived.2"));
        assertTrue(objProps.containsKey("bArrayDerived.3"));
        assertTrue(objProps.containsKey("aStringDerived"));
        assertTrue(objProps.containsKey("aFloatDerived"));
        assertTrue(objProps.containsKey("aIntDerived"));
        assertTrue(objProps.containsKey("cArrayDerivedAll"));
        assertTrue(objProps.containsKey("cArrayDerived.1"));
        assertTrue(objProps.containsKey("cArrayDerived.2"));
        assertTrue(objProps.containsKey("cArrayDerived.3"));
        assertTrue(objProps.containsKey("cArrayDerived.4"));

        assertTrue(objProps.containsKey("bArray.1"));
        assertTrue(objProps.containsKey("bArray.2"));
        assertTrue(objProps.containsKey("bArray.3"));
        assertTrue(objProps.containsKey("aString"));
        assertTrue(objProps.containsKey("aFloat"));
        assertTrue(objProps.containsKey("aInt"));
        assertTrue(objProps.containsKey("cArrayAll"));
        assertTrue(objProps.containsKey("cArray.1"));
        assertTrue(objProps.containsKey("cArray.2"));
        assertTrue(objProps.containsKey("cArray.3"));
        assertTrue(objProps.containsKey("cArray.4"));

        assertFalse(objProps.containsKey("NotContained"));

        assertEquals(String.valueOf(objectClass.bArrayDerived[0]), (String) objProps.get("bArrayDerived.1"));
        assertEquals(String.valueOf(objectClass.bArrayDerived[1]), (String) objProps.get("bArrayDerived.2"));
        assertEquals(String.valueOf(objectClass.bArrayDerived[2]), (String) objProps.get("bArrayDerived.3"));
        assertEquals(objectClass.aStringDerived, (String) objProps.get("aStringDerived"));
        assertEquals(String.valueOf(objectClass.aFloatDerived), (String) objProps.get("aFloatDerived"));
        assertEquals(String.valueOf(objectClass.aIntDerived), (String) objProps.get("aIntDerived"));
        assertEquals(String.valueOf(objectClass.cArrayDerivedAll), (String) objProps.get("cArrayDerivedAll"));
        assertEquals(String.valueOf(objectClass.cArrayDerived[0]), (String) objProps.get("cArrayDerived.1"));
        assertEquals(String.valueOf(objectClass.cArrayDerived[1]), (String) objProps.get("cArrayDerived.2"));
        assertEquals(String.valueOf(objectClass.cArrayDerived[2]), (String) objProps.get("cArrayDerived.3"));
        assertEquals(String.valueOf(objectClass.cArrayDerived[3]), (String) objProps.get("cArrayDerived.4"));

        assertEquals(String.valueOf(objectClass.bArray[0]), (String) objProps.get("bArray.1"));
        assertEquals(String.valueOf(objectClass.bArray[1]), (String) objProps.get("bArray.2"));
        assertEquals(String.valueOf(objectClass.bArray[2]), (String) objProps.get("bArray.3"));
        assertEquals(objectClass.aString, (String) objProps.get("aString"));
        assertEquals(String.valueOf(objectClass.aFloat), (String) objProps.get("aFloat"));
        assertEquals(String.valueOf(objectClass.aInt), (String) objProps.get("aInt"));
        assertEquals(String.valueOf(objectClass.cArrayAll), (String) objProps.get("cArrayAll"));
        assertEquals(String.valueOf(objectClass.cArray[0]), (String) objProps.get("cArray.1"));
        assertEquals(String.valueOf(objectClass.cArray[1]), (String) objProps.get("cArray.2"));
        assertEquals(String.valueOf(objectClass.cArray[2]), (String) objProps.get("cArray.3"));
        assertEquals(String.valueOf(objectClass.cArray[3]), (String) objProps.get("cArray.4"));
    }

    public void testGetObjectMetadata() {
        final TestObjectClass objectClass = new TestObjectClass();
        final MetadataElement metadata = ObjectIO.getObjectMetadata(objectClass);

        assertNotNull(metadata);
        assertEquals(11, metadata.getNumAttributes());
        assertTrue(metadata.containsAttribute("bArray.1"));
        assertTrue(metadata.containsAttribute("bArray.2"));
        assertTrue(metadata.containsAttribute("bArray.3"));
        assertTrue(metadata.containsAttribute("aString"));
        assertTrue(metadata.containsAttribute("aFloat"));
        assertTrue(metadata.containsAttribute("aInt"));
        assertTrue(metadata.containsAttribute("cArrayAll"));
        assertTrue(metadata.containsAttribute("cArray.1"));
        assertTrue(metadata.containsAttribute("cArray.2"));
        assertTrue(metadata.containsAttribute("cArray.3"));
        assertTrue(metadata.containsAttribute("cArray.4"));

        assertFalse(metadata.containsAttribute("NotContained"));

        assertEquals(String.valueOf(objectClass.bArray[0]), metadata.getAttribute("bArray.1").getData().getElemString());
        assertEquals(String.valueOf(objectClass.bArray[1]), metadata.getAttribute("bArray.2").getData().getElemString());
        assertEquals(String.valueOf(objectClass.bArray[2]), metadata.getAttribute("bArray.3").getData().getElemString());
        assertEquals(objectClass.aString, metadata.getAttribute("aString").getData().getElemString());
        assertEquals(String.valueOf(objectClass.aFloat), metadata.getAttribute("aFloat").getData().getElemString());
        assertEquals(String.valueOf(objectClass.aInt), metadata.getAttribute("aInt").getData().getElemString());
        assertEquals(String.valueOf(objectClass.cArrayAll), metadata.getAttribute("cArrayAll").getData().getElemString());
        assertEquals(String.valueOf(objectClass.cArray[0]), metadata.getAttribute("cArray.1").getData().getElemString());
        assertEquals(String.valueOf(objectClass.cArray[1]), metadata.getAttribute("cArray.2").getData().getElemString());
        assertEquals(String.valueOf(objectClass.cArray[2]), metadata.getAttribute("cArray.3").getData().getElemString());
        assertEquals(String.valueOf(objectClass.cArray[3]), metadata.getAttribute("cArray.4").getData().getElemString());

    }

    public static class TestObjectClass{
        public boolean[] bArray = new boolean[] {true, false, true};
        public String aString = "Honolulu";
        public float aFloat = 666.66f;
        public int aInt = 42;
        public boolean cArrayAll;
        public boolean[] cArray = new boolean[]{false, false, false, false};
    }

    public static class TestDerivedObjectClass extends TestObjectClass{
        public TestDerivedObjectClass() {
            aString = "Sansibar";     // reinitializes field in super class        }
        }

        public boolean[] bArrayDerived = new boolean[] {true, false, true};
        public String aStringDerived = "Hawaii";
        public float aFloatDerived = 77.2f;
        public int aIntDerived = 12;
        public boolean cArrayDerivedAll;
        public boolean[] cArrayDerived = new boolean[]{false, false, false, false};
    }

}