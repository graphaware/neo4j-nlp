package com.graphaware.nlp.parser;

import com.graphaware.nlp.parser.domain.Page;

import java.io.InputStream;
import java.util.List;

public interface Parser {

    List<Page> parse(InputStream fs, List<String> filterPatterns) throws Exception;

}
