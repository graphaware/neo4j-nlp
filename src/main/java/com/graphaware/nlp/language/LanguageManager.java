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
package com.graphaware.nlp.language;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.graphaware.nlp.domain.Constants.LANGUAGE_EN;
import static com.graphaware.nlp.domain.Constants.LANGUAGE_NA;

/*
* https://github.com/optimaize/language-detector
 */
public class LanguageManager {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageManager.class);
    private boolean initialized = false;
    private LanguageDetector languageDetector;
    private TextObjectFactory textObjectFactory;
    
    private final Set<String> supportedLanguages = new TreeSet<>();

    private LanguageManager() {
        supportedLanguages.add(LANGUAGE_EN);
    }

    private void initialize() {
        LOG.info("Initializing Language Detector ...");
        try {
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
            //build language detector:
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
            //create a text object factory
            textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
            initialized = true;
        } catch (IOException ex) {
            initialized = false;
            LOG.error("Error while initializing Language Detector", ex);
        }
    }

    public static LanguageManager getInstance() {
        return LanguageDetectionHolder.INSTANCE;
    }

    private static class LanguageDetectionHolder {

        private static final LanguageManager INSTANCE = new LanguageManager();
    }

    public String detectLanguage(String text) {
        if (!initialized) {
            initialize();
        }
        if (text != null) {
            TextObject textObject = textObjectFactory.forText(text);
            Optional<LdLocale> lang = languageDetector.detect(textObject);
            if (lang.isPresent()) {
                return lang.get().getLanguage();
            }
        }

        return LANGUAGE_NA;
    }
    
    public boolean isTextLanguageSupported(String text) {
        return supportedLanguages.contains(detectLanguage(text));
    }
    
    public boolean isLanguageSupported(String language) {
        return supportedLanguages.contains(language);
    }
    
    public void addSupportedLanguage(String language) {
        supportedLanguages.add(language);        
    }

}
