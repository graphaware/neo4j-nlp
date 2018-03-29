package com.graphaware.nlp.util;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.configuration.SettingsConstants;
import org.neo4j.logging.Log;

public class ProcessorUtils {

    private static final Log LOG = LoggerFactory.getLogger(ProcessorUtils.class);

    public static String getPipeline(String pipelineName, DynamicConfiguration configuration) {

        if (pipelineName != null) {
            return pipelineName;
        }

        if (configuration.hasSettingValue(SettingsConstants.DEFAULT_PIPELINE)) {
            LOG.info("Taking default pipeline from configuration : " + configuration.getSettingValueFor(SettingsConstants.DEFAULT_PIPELINE).toString());
            return configuration.getSettingValueFor(SettingsConstants.DEFAULT_PIPELINE).toString();
        }

        throw new RuntimeException("A pipeline should be given or set as default");
    }

}
