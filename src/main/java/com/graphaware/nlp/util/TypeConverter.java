/*
 * Copyright (c) 2013-2018 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

public class TypeConverter {

    private static final Log LOG = LoggerFactory.getLogger(TypeConverter.class);

    public static byte[] toByteArray(double[] doubleArray) {
        int times = Double.SIZE / Byte.SIZE;
        byte[] bytes = new byte[doubleArray.length * times];
        for (int i = 0; i < doubleArray.length; i++) {
            ByteBuffer.wrap(bytes, i * times, times).putDouble(doubleArray[i]);
        }
        return bytes;
    }

    public static byte[] toByteArray(float[] floats) {
        int times = Float.SIZE / Byte.SIZE;
        byte[] bytes = new byte[floats.length * times];
        for (int i = 0; i < floats.length; i++) {
            ByteBuffer.wrap(bytes, i * times, times).putFloat(floats[i]);
        }
        return bytes;
    }

    public static double[] toDoubleArray(byte[] byteArray) {
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = ByteBuffer.wrap(byteArray, i * times, times).getDouble();
        }
        return doubles;
    }

    public static float[] toFloatArray(byte[] byteArray) {
        int times = Float.SIZE / Byte.SIZE;
        float[] floats = new float[byteArray.length / times];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = ByteBuffer.wrap(byteArray, i * times, times).getFloat();
        }
        return floats;
    }

    public static String[] convertStringListToArray(List<String> list) {
        return list.toArray(new String[0]);
    }

    public static double getDoubleValue(Object value) {
        if (value == null) {
            return 1.0d;
        }
        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }
        if (value instanceof Double) {
            return ((Double) value);
        } else {
            try {
                return Double.valueOf(String.valueOf(value));
            } catch (Exception ex) {
                LOG.error("Error while parsing double value from string: " + value, ex);
                return 1.0d;
            }
        }
    }
    
    public static float getFloatValue(Object value) {
        if (value == null) {
            return 1.0f;
        }
        if (value instanceof Double) {
            return ((Double) value).floatValue();
        }
        if (value instanceof Float) {
            return ((Float) value);
        } else {
            try {
                return Float.valueOf(String.valueOf(value));
            } catch (Exception ex) {
                LOG.error("Error while parsing float value from string: " + value, ex);
                return 1.0f;
            }
        }
    }

    public static int getIntegerValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        } 
        if (value instanceof Float) {
            return ((Float) value).intValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value);
        } else {
            try {
                return Integer.valueOf(String.valueOf(value));
            } catch (Exception ex) {
                LOG.error("Error while parsing float value from string: " + value, ex);
                return 0;
            }
        }
    }

    public static Long toLong(Object value) {
        Long returnValue;
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            returnValue = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            returnValue = ((Long) value);
        } else if (value instanceof String) {
            returnValue = Long.parseLong((String) value);
        } else {
            throw new RuntimeException("Value: " + value + " cannot be cast to Long");
        }
        return returnValue;
    }

    public static <T> List<T> iterableToList(Iterable<T> it) {
        List<T> newList = new ArrayList<>();
        for (T obj : it) {
            newList.add(obj);
        }
        return newList;
    }
}
