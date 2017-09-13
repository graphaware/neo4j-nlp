package com.graphaware.nlp.dsl.request;

import java.util.List;
import java.util.Map;

public interface ProcedureRequest {

    List<String> validMapKeys();

    List<String> mandatoryKeys();

    void validateMap(Map<String, Object> map);
}
