package com.graphaware.nlp.util;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.processor.TextProcessor;
import org.neo4j.logging.Log;

public class ProcessorUtils {

    private static final Log LOG = LoggerFactory.getLogger(ProcessorUtils.class);

    public static String getPipeline(String pipelineName, DynamicConfiguration configuration) {
        if (pipelineName == null && !configuration.hasSettingValue(SettingsConstants.DEFAULT_PIPELINE)) {
            LOG.info("Defaulting to default pipeline : " + SettingsConstants.DEFAULT_PIPELINE);
            return TextProcessor.DEFAULT_PIPELINE;
        }

        if (pipelineName == null && configuration.hasSettingValue(SettingsConstants.DEFAULT_PIPELINE)) {
            LOG.info("Taking default pipeline from configuration : " + configuration.getSettingValueFor(SettingsConstants.DEFAULT_PIPELINE).toString());
            return configuration.getSettingValueFor(SettingsConstants.DEFAULT_PIPELINE).toString();
        }

        return pipelineName;
    }

}
