package com.graphaware.nlp.dsl.request;

import com.graphaware.nlp.util.ProcedureRequestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractProcedureRequest implements ProcedureRequest {

    @Override
    public List<String> mandatoryKeys() {
        return Arrays.asList();
    }

    @Override
    public void validateMap(Map<String, Object> map) {
        map.keySet().forEach(key -> {
            String v = ProcedureRequestUtils.hasPossibleRequestKey(key, validMapKeys());
            if (v != null) {
                throw new RuntimeException(String.format("Invalid request parameter '%s', maybe you meant '%s' instead ?", key, v));
            }
        });
    }

    protected void validateRequestHasMandatoryKeys(List<String> keys, Map<String, Object> map) {
        keys.forEach(key -> {
            if (!map.containsKey(key)) {
                throw new RuntimeException(String.format("Missing key '%s'", key));
            }
        });
    }

    protected void validateRequestHasKeyOrOtherKey(String key1, String key2, Map<String, Object> map) {
        if (!map.containsKey(key1) && !map.containsKey(key2)) {
            throw new RuntimeException(String.format("Missing key, you should specify '%s' or '%s'", key1, key2));
        }
    }
}
