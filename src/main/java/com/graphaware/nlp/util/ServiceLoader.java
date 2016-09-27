/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.util;

import com.graphaware.nlp.processor.TextProcessor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
public class ServiceLoader {

    private static Reflections reflections;
    private static final Logger LOG = LoggerFactory.getLogger(ServiceLoader.class);

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
