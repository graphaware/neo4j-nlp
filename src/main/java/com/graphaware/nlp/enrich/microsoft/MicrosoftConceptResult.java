package com.graphaware.nlp.enrich.microsoft;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MicrosoftConceptResult {

    private List<Map<String, Object>> ArrayOfKeyValueOfstringdouble = new ArrayList<>();

    public List<Map<String, Object>> getArrayOfKeyValueOfstringdouble() {
        return ArrayOfKeyValueOfstringdouble;
    }
}
