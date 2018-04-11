package com.graphaware.nlp.parser;

import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.parser.domain.Page;
import com.graphaware.nlp.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public abstract class AbstractParser extends AbstractExtension implements Parser {

    public List<Page> parse(String filename, List<String> filterPatterns) throws Exception {
        return parse(getFileStream(filename), filterPatterns);
    }

    protected InputStream getFileStream(String filename) throws Exception {

        String path = FileUtils.getFileUri(filename);
        if (path.startsWith("http")) {
            URL url = new URL(path);
            return url.openStream();
        }

        return new FileInputStream(new File(path));
    }

}
