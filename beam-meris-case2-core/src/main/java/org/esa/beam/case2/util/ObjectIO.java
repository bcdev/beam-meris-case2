package org.esa.beam.case2.util;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * A utility class which is used to read objects.
 */
public class ObjectIO {

    private ObjectIO() {
    }

    /**
     * Creates an instance of the given type and sets the fields of the resulting object with the values found in the
     * specified properties file. (For a description of the properties file format refer to {@link java.util.Properties#load}).
     * <p>The method can handle fields of all primitive Java types as well as <code>String</code> and <code>boolean</code> arrays.
     *
     * @param type     the type of the object to be read
     * @param filePath the file path to the properties file
     *
     * @return the resulting object
     *
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if any field of the given object is not accessible or any
     *                                  value in the property map is not convertible to the required field type.
     */
    public static <T> T readObject(final Class<T> type, final String filePath) throws IOException {
        final InputStream inputStream = new FileInputStream(filePath);
        try {
            return readObject(type, inputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * Creates an instance of the given type and sets the fields of the resulting object with the values found in the
     * property list (key and element pairs) given as input stream. (For a description of the properties file format refer to {@link java.util.Properties#load}).
     * <p>The method can handle fields of all primitive Java types as well as <code>String</code> and <code>boolean</code> arrays.
     *
     * @param type        the type of the object to be read
     * @param inputStream the input stream
     *
     * @return the resulting object
     *
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if any field of the given object is not accessible or any
     *                                  value in the property map is not convertible to the required field type.
     */
    public static <T> T readObject(final Class<T> type, final InputStream inputStream) throws IOException {
        final T object = createInstance(type);
        final Properties properties = new Properties();
        properties.load(inputStream);
        setObjectProperties(object, properties);
        checkPropertiesForCorrespondingField(object, properties);
        return object;
    }

    private static <T> T createInstance(Class<T> type) {
        final T object;
        try {
            object = type.newInstance();
        } catch (InstantiationException e) {
            throw createIllegalArgError(type, e);
        } catch (IllegalAccessException e) {
            throw createIllegalArgError(type, e);
        }
        return object;
    }

    private static void checkPropertiesForCorrespondingField(Object object, Properties properties) throws IOException {
        Field[] fields = object.getClass().getFields();
        Set<Object> propertyNames = properties.keySet();
        Iterator iterator = propertyNames.iterator();
        List<Object> wrongProperties = new ArrayList<Object>(propertyNames);
        while (iterator.hasNext()) {
            String propertyName = (String) iterator.next();
            for (Field field : fields) {
                if (propertyName.startsWith(field.getName())) {
                    wrongProperties.remove(propertyName);
                    break;
                }
            }
        }
        if (!wrongProperties.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object wrongProperty : wrongProperties) {
                sb.append("\n");
                sb.append(wrongProperty);
            }
            throw new IOException("Not able to find a field for one or more properties: \n" + sb);
        }
    }

    /**
     * Sets the field of the given object to the values found in the
     * given property list (key and element pairs).
     *
     * @param object     the object
     * @param properties the property list (key and element pairs)
     *
     * @throws IllegalArgumentException if any field of the given object is not accessible or any
     *                                  value in the property map is not convertible to the required field type.
     */
    public static void setObjectProperties(final Object object, final Properties properties) {
        final Field[] fields = object.getClass().getFields();
        for (final Field field : fields) {
            final String propertyName = field.getName();
            if (field.getType().isArray()) {
                if (field.getType().equals(boolean[].class)) {
                    final Object arrayValue = getFieldValue(field, object);

                    final Field fieldAll = getField(fields, propertyName + "All");
                    if (fieldAll != null) {
                        final String valueString = properties.getProperty(fieldAll.getName());
                        final Boolean value = Boolean.valueOf(valueString);
                        setFieldValue(fieldAll, object, value);
                        Arrays.fill((boolean[]) arrayValue, value);
                    }

                    for (int j = 0; j < Array.getLength(arrayValue); j++) {
                        final String elementName = MessageFormat.format("{0}.{1}", propertyName, j + 1);
                        final String elementValue = properties.getProperty(elementName);
                        if (elementValue != null) {
                            boolean value = Boolean.valueOf(elementValue);
                            Array.set(arrayValue, j, value);
                        }
                    }
                } else {
                    // ignore all arrays which are not boolean[]
                }
            } else {
                final String propertyValueStr = properties.getProperty(propertyName);
                if (propertyValueStr != null) {
                    try {
                        if (field.getType().equals(boolean.class)) {
                            setFieldValue(field, object, Boolean.valueOf(propertyValueStr));
                        } else if (field.getType().equals(byte.class)) {
                            setFieldValue(field, object, Byte.valueOf(propertyValueStr));
                        } else if (field.getType().equals(short.class)) {
                            setFieldValue(field, object, Short.valueOf(propertyValueStr));
                        } else if (field.getType().equals(int.class)) {
                            setFieldValue(field, object, Integer.valueOf(propertyValueStr));
                        } else if (field.getType().equals(long.class)) {
                            setFieldValue(field, object, Long.valueOf(propertyValueStr));
                        } else if (field.getType().equals(float.class)) {
                            setFieldValue(field, object, Float.valueOf(propertyValueStr));
                        } else if (field.getType().equals(double.class)) {
                            setFieldValue(field, object, Double.valueOf(propertyValueStr));
                        } else if (field.getType().equals(String.class)) {
                            setFieldValue(field, object, propertyValueStr);
                        } else {
                            // ignore all other objects
                        }
                    } catch (NumberFormatException e) {
                        throw createIllegalArgError(object.getClass(), field, e);
                    }
                }
            }
        }

    }

    private static Field getField(Field[] fields, String fieldName) {
        for (final Field field1 : fields) {
            if (field1.getName().equals(fieldName)) {
                return field1;
            }
        }
        return null;
    }

    /**
     * Returns the field values of the given object in form of {@link Properties properties} (key value pairs).
     *
     * @param object the object
     *
     * @return the keys (field name) and values (field value) of the given object as {@link Properties properties}.
     */
    public static Properties getObjectProperties(final Object object) {
        final Properties properties = new Properties();
        Class<?> type = object.getClass();
        while (!type.equals(Object.class)) {
            collectFieldProperties(object, type, properties);
            type = type.getSuperclass();
        }
        return properties;
    }

    /*
    Collects the declared fields of the given object for the given type and stores them
    in the properties parameter if not already contained
     */

    private static void collectFieldProperties(Object object, final Class<?> type, Properties properties) {
        final Field[] fields = type.getDeclaredFields();
        for (final Field field : fields) {
            final String propertyName = field.getName();
            if (field.getType().isArray()) {
                if (field.getType().equals(boolean[].class)) {
                    final Object arrayValue = getFieldValue(field, object);
                    for (int j = 0; j < Array.getLength(arrayValue); j++) {
                        final String elementName = MessageFormat.format("{0}.{1}", propertyName, j + 1);
                        final boolean aBoolean = Array.getBoolean(arrayValue, j);
                        properties.setProperty(elementName, Boolean.toString(aBoolean));
                    }
                } else {
                    // ignore all arrays which are not boolean[]
                }
            } else {
                final ProductData propertyData;
                try {
                    if (field.getType().equals(boolean.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance(fieldValue.toString());
                    } else if (field.getType().equals(byte.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance(ProductData.TYPE_INT8, 1);
                        propertyData.setElemInt((Byte) fieldValue);
                    } else if (field.getType().equals(short.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance(ProductData.TYPE_INT16, 1);
                        propertyData.setElemInt((Short) fieldValue);
                    } else if (field.getType().equals(int.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance(ProductData.TYPE_INT32, 1);
                        propertyData.setElemInt((Integer) fieldValue);
                    } else if (field.getType().equals(long.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance(ProductData.TYPE_FLOAT64, 1);
                        propertyData.setElemDouble((Long) fieldValue);
                    } else if (field.getType().equals(float.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance(ProductData.TYPE_FLOAT32, 1);
                        propertyData.setElemFloat((Float) fieldValue);
                    } else if (field.getType().equals(double.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance(ProductData.TYPE_FLOAT64, 1);
                        propertyData.setElemDouble((Double) fieldValue);
                    } else if (field.getType().equals(String.class)) {
                        Object fieldValue = getFieldValue(field, object);
                        propertyData = ProductData.createInstance((String) fieldValue);
                    } else {
                        // ignore all other objects
                        continue;
                    }
                } catch (NumberFormatException e) {
                    throw createIllegalArgError(object.getClass(), field, e);
                }
                if (!properties.containsKey(propertyName)) {
                    properties.setProperty(propertyName, propertyData.getElemString());
                }

            }
        }
    }

    public static MetadataElement getObjectMetadata(final Object object) {
        final Properties objectProperties = getObjectProperties(object);
        final MetadataElement metadata = new MetadataElement("Processor Parameter");
        final Enumeration<?> enumeration = objectProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            final String propertyName = (String) enumeration.nextElement();
            final String propertyValue = objectProperties.getProperty(propertyName);
            final MetadataAttribute attribute = new MetadataAttribute(propertyName,
                                                                      ProductData.createInstance(propertyValue),
                                                                      true);
            metadata.addAttribute(attribute);
        }
        return metadata;
    }

    private static void setFieldValue(final Field field, final Object object, final Object value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw createIllegalArgError(object.getClass(), field, e);
        }
    }

    private static Object getFieldValue(final Field field, final Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw createIllegalArgError(object.getClass(), field, e);
        }
    }

    private static IllegalArgumentException createIllegalArgError(final Class type, final Field field, Exception e) {
        return new IllegalArgumentException("Illegal type: " + type.getClass().getName() +
                                            ": cannot access parameter '" + field.getName() + "'", e);
    }

    private static IllegalArgumentException createIllegalArgError(final Class type, Exception e) {
        return new IllegalArgumentException("Illegal type: " + type.getClass().getName(), e);
    }


}
