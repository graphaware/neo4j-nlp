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

    @Procedure(name = "ga.nlp.config.setAll", mode = Mode.WRITE)
    @Description("Set all the given user defined configuration settings")
    public Stream<SingleResult> setAllConfigValues(@Name("config") Map<String, Object> config) {
        config.keySet().forEach(k -> {
            getNLPManager().getConfiguration().update(k, config.get(k));
        });

        return Stream.of(SingleResult.success());
    }




}
