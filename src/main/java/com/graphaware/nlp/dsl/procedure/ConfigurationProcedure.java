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
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.KeyValueResult;
import com.graphaware.nlp.dsl.result.SingleResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

public class ConfigurationProcedure extends AbstractDSL {

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

    @Procedure(name = "ga.nlp.config.setAll", mode = Mode.WRITE)
    @Description("Set all the given user defined configuration settings")
    public Stream<SingleResult> setAllConfigValues(@Name("config") Map<String, Object> config) {
        config.keySet().forEach(k -> {
            getNLPManager().getConfiguration().update(k, config.get(k));
        });

        return Stream.of(SingleResult.success());
    }
}
