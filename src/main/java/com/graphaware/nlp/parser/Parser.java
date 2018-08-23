package com.graphaware.nlp.parser;

import com.graphaware.nlp.parser.domain.Page;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface Parser {

    List<Page> parse(InputStream fs, List<String> filterPatterns) throws Exception;

    List<Page> parse(String filename, List<String> filterPatterns) throws Exception;

    List<Page> parse(String filename, List<String> filterPatterns, Map<String, Object> parserOptions) throws Exception;

}
