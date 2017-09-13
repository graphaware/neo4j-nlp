/*
 * Copyright (c) 2013-2017 GraphAware
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

import com.graphaware.nlp.extension.NLPExtension;
import com.graphaware.nlp.processor.TextProcessor;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServiceLoader {

    private static Reflections reflections;
    private static final Logger LOG = LoggerFactory.getLogger(ServiceLoader.class);

    public static NLPExtension loadNLPExtension(String extensionClazz) {
        NLPExtension extension;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends NLPExtension> clazz = (Class<? extends NLPExtension>) Class
                    .forName(extensionClazz);
            NLPExtension classInstance = clazz.newInstance();

            if (classInstance instanceof TextProcessor) {
                extension = (NLPExtension) classInstance;
                //datumSerializer.configure(filterContext);
            } else {
                throw new IllegalArgumentException(extensionClazz
                        + " is not an NLP Extension");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
            LOG.error("Could not instantiate event filter.", e);
            throw new RuntimeException("Could not instantiate event filter.", e);
        }
        return extension;
    }

    public static TextProcessor loadTextProcessor(String processorClazz) {
        TextProcessor processor;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends TextProcessor> clazz = (Class<? extends TextProcessor>) Class
                    .forName(processorClazz);
            TextProcessor classInstance = clazz.newInstance();

            if (classInstance instanceof TextProcessor) {
                processor = (TextProcessor) classInstance;
                //datumSerializer.configure(filterContext);
            } else {
                throw new IllegalArgumentException(processorClazz
                        + " is not an TextProcessor");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
            LOG.error("Could not instantiate event filter.", e);
            throw new RuntimeException("Could not instantiate event filter.", e);
        }
        return processor;
    }

    public static <T> Map<String, T> loadInstances(Class<? extends Annotation> annotationClass) {
        Map<String, Class<T>> loadedClass = loadClass(annotationClass);
        Map<String, T> result = new HashMap<>();

        if (loadedClass == null) {
            return result;
        }

        loadedClass.entrySet().forEach(entry -> {
            try {
                LOG.info("Loading text processor: " + entry.getKey() + " with class: " + entry.getValue().getName());
                Class<T> cls = entry.getValue();
                Constructor<T> constructor = cls.getConstructor();
                T newInstance = constructor.newInstance();
                result.put(entry.getKey(), newInstance);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                LOG.error("Error while initializing " + entry.getKey() + ". Continuing");
                ex.printStackTrace();
            }
        });



        return result;
    }

    public static <T, A extends Annotation> Map<String, Class<T>> loadClass(Class<A> annotation) {
        return loadClassByAnnotation(annotation);
    }

    private static <T, A extends Annotation> Map<String, Class<T>> loadClassByAnnotation(Class<A> annotation) {
        if (reflections == null) {
            loadReflections("com.graphaware");
        }
        Map<String, Class<T>> loader = new HashMap<>();
        Set<Class<?>> providers = reflections.getTypesAnnotatedWith(annotation);
        providers.stream().forEach((item) -> {
            loader.put(item.getName(), (Class<T>) item);
        });
        return loader;
    }

    private static void loadReflections(final String packagePath) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                reflections = new Reflections(packagePath);
                return null; // nothing to return
            }
        });
    }

    private ServiceLoader() {

    }
}
