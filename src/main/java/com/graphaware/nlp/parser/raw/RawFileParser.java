package com.graphaware.nlp.parser.raw;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.extension.AbstractExtension;

import java.nio.file.Files;
import java.nio.file.Paths;

@NLPModuleExtension(name = "ga.nlp.parser.raw")
public class RawFileParser extends AbstractExtension {

    public String parse(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

}
