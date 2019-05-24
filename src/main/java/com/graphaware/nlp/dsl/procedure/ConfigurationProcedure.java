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
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.KeyValueResult;
import com.graphaware.nlp.dsl.result.SingleResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ConfigurationProcedure extends AbstractDSL {

    private static Map<String, String> languagesMap;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("english", "en");
        map.put("german", "de");
        map.put("french", "fr");
        map.put("arabic", "ar");
        map.put("spanish", "es");
        map.put("chinese", "zh");
        languagesMap = map;
    }

    @Procedure(name = "ga.nlp.config.show", mode = Mode.READ)
    @Description("Show the NLP Module Configuration settings")
    public Stream<KeyValueResult> showConfig() {
        Map<String, Object> config = getNLPManager().getConfiguration().getAllConfigValuesFromStore();

        return config.keySet().stream().map(k -> {
            return new KeyValueResult(k, config.get(k));
        });
    }

    @Procedure(name = "ga.nlp.config.set", mode = Mode.WRITE)
    @Description("Set a user defined configuration setting")
    public Stream<SingleResult> setConfigValue(@Name("key") String key, @Name("value") Object value) {
        getNLPManager().getConfiguration().update(key, value);

        return Stream.of(SingleResult.success());
    }

    @Procedure(name = "ga.nlp.config.setting.set", mode = Mode.WRITE)
    public Stream<SingleResult> setSettingValue(@Name("key") String key, @Name("value") Object value) {
        getNLPManager().getConfiguration().updateInternalSetting(key, value);

        return Stream.of(SingleResult.success());
    }

    @Procedure(name = "ga.nlp.config.remove", mode = Mode.WRITE)
    public Stream<SingleResult> removeSettingValue(@Name("key") String key) {
        getNLPManager().getConfiguration().removeValue(key);

        return Stream.of(SingleResult.success());
    }

    @Procedure(name = "ga.nlp.config.setAll", mode = Mode.WRITE)
    @Description("Set all the given user defined configuration settings")
    public Stream<SingleResult> setAllConfigValues(@Name("config") Map<String, Object> config) {
        config.keySet().forEach(k -> {
            getNLPManager().getConfiguration().update(k, config.get(k));
        });

        return Stream.of(SingleResult.success());
    }

    @Procedure(name = "ga.nlp.config.model.workdir", mode = Mode.WRITE)
    @Description("Defines the default working directory for custom processor models")
    public Stream<SingleResult> setDefaultWorkdir(@Name("path") String path) {
        getNLPManager().getConfiguration().updateInternalSetting(SettingsConstants.DEFAULT_MODEL_WORKDIR, path);

        return Stream.of(SingleResult.success());
    }

    @Procedure(name = "ga.nlp.config.model.add", mode = Mode.WRITE)
    @Description("Register an existing model available for use in the system")
    public Stream<SingleResult> addModel(@Name("modelId") String id, @Name("modelPath") String path) {
        getNLPManager().addModel(id, path);

        return Stream.of(SingleResult.success());
    }

    @Procedure(name = "ga.nlp.config.model.list", mode = Mode.WRITE)
    @Description("Returns all the model paths stored in the database")
    public Stream<KeyValueResult> listModels() {
        List<KeyValueResult> results = new ArrayList<>();
        Map<String, String> models = getConfiguration().getAllModelPaths();
        models.keySet().forEach(k -> {
            results.add(new KeyValueResult(k, models.get(k)));
        });

        return results.stream();
    }

    @Procedure(name = "ga.nlp.config.setDefaultLanguage", mode = Mode.WRITE)
    @Description("Defines the default language for Text Analysis pipelines")
    public Stream<SingleResult> setDefaultLanguage(@Name("language") String s) {
        String lang = languagesMap.containsKey(s.toLowerCase())
                ? languagesMap.get(s.toLowerCase())
                : s.toLowerCase();
        getNLPManager().getConfiguration().updateInternalSetting(SettingsConstants.FALLBACK_LANGUAGE, lang);

        return Stream.of(SingleResult.success());
    }
}
