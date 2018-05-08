package com.graphaware.nlp.vector;

import com.graphaware.common.log.LoggerFactory;
import org.neo4j.logging.Log;

public class VectorFactory {

    private static final Log LOG = LoggerFactory.getLogger(VectorFactory.class);

    public static VectorHandler createVector(String type, float[] vector) {

        try {
            Class<? extends GenericVector> clazz = (Class<? extends GenericVector>) Class
                    .forName(type);
            GenericVector actualVector = clazz.newInstance();
            actualVector.setArray(vector);
            return new VectorHandler(type, actualVector);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            String message = "Error while creating the vector of type: " + type;
            LOG.error(message, ex);
            throw new RuntimeException(message, ex);
        } 
    }
}
