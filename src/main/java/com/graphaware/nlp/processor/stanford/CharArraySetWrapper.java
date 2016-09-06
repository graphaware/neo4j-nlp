/*
 * Copyright (c) 2013-2016 GraphAware
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
package com.graphaware.nlp.processor.stanford;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharArraySetWrapper {

    private final static Logger LOG = LoggerFactory.getLogger(CharArraySetWrapper.class);

    private Object stopwords;

    public CharArraySetWrapper(Object stopwords) {
        this.stopwords = stopwords;
    }

    
    public CharArraySetWrapper() {
        try {
            final Class<?> stopWordClass = findStopAnalyzerClass();
            Field staticField = stopWordClass.getDeclaredField("ENGLISH_STOP_WORDS_SET");
            stopwords = staticField.get(findCharArraySetClass());
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        } 
    }

    public CharArraySetWrapper(int length, boolean ignoreCase) {
        Class<?> charArraySetClass = findCharArraySetClass();

        try {
            if (charArraySetClass != null) {
                Constructor constructor = charArraySetClass.getConstructor(Integer.TYPE, Boolean.TYPE);
                stopwords = constructor.newInstance(length, ignoreCase);
            } else {
                throw new RuntimeException("Cannot initialize CharArraySetWrapper");
            }
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }

    }

    private static Class<?> findCharArraySetClass() {
        Class<?> charArraySetClass = null;
        try {
            charArraySetClass = Class.forName("org.apache.lucene.analysis.util.CharArraySet");

        } catch (ClassNotFoundException ex) {
            LOG.warn("Not found org.apache.lucene.analysis.util.CharArraySet try looking into org.apache.lucene.analysis.CharArraySet");
            try {
                charArraySetClass = Class.forName("org.apache.lucene.analysis.CharArraySet");
            } catch (ClassNotFoundException ex1) {
                throw new RuntimeException(ex1);
            }
        }
        return charArraySetClass;
    }
    
    private static Class<?> findStopAnalyzerClass() {
        Class<?> charArraySetClass = null;
        try {
            charArraySetClass = Class.forName("org.apache.lucene.analysis.core.StopAnalyzer");

        } catch (ClassNotFoundException ex) {
            LOG.warn("Not found org.apache.lucene.analysis.util.CharArraySet try looking into org.apache.lucene.analysis.CharArraySet");
            try {
                charArraySetClass = Class.forName("org.apache.lucene.analysis.StopAnalyzer");
            } catch (ClassNotFoundException ex1) {
                throw new RuntimeException(ex1);
            }
        }
        return charArraySetClass;
    }
    
    

    public void add(String term) {
        try {
            Method method = stopwords.getClass().getMethod("add", String.class);
            method.invoke(stopwords, term);
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static CharArraySetWrapper unmodifiableSet(CharArraySetWrapper stopwordSet) {
        try {
            final Class<?> stopWordClass = findCharArraySetClass();
            Method declaredMethod = stopWordClass.getDeclaredMethod("unmodifiableSet", stopWordClass);
            Object newStopWords = declaredMethod.invoke(null, stopwordSet.stopwords);
            return new CharArraySetWrapper(newStopWords);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } 
    }

    public int size() {
        try {
            Method method = stopwords.getClass().getMethod("size");
            Integer result = (Integer) method.invoke(stopwords);
            return result;
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contains(String toLowerCase) {
        try {
            Method method = stopwords.getClass().getMethod("contains", CharSequence.class);
            Boolean result = (Boolean) method.invoke(stopwords, toLowerCase);
            return result;
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
