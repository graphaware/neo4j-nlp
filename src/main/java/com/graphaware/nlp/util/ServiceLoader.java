/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.util;

import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
public class ServiceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(StanfordTextProcessor.class);

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
}
