package com.graphaware.nlp.util;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.processor.TextProcessor;

public class ProcessorUtils {

    public static String getPipeline(String pipelineName, DynamicConfiguration configuration) {
        if (pipelineName == null && !configuration.hasSettingValue(SettingsConstants.DEFAULT_PIPELINE)) {
            return TextProcessor.DEFAULT_PIPELINE;
        }

        if (pipelineName == null && configuration.hasSettingValue(SettingsConstants.DEFAULT_PIPELINE)) {
            return configuration.getSettingValueFor(SettingsConstants.DEFAULT_PIPELINE).toString();
        }

        return pipelineName;
    }

}
