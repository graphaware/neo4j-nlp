package com.graphaware.nlp.parser;

import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.parser.domain.Page;
import com.graphaware.nlp.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractParser extends AbstractExtension implements Parser {

    public static final String USER_AGENT_OPTION = "UserAgent";

    public List<Page> parse(String filename, List<String> filterPatterns) throws Exception {
        return parse(getFileStream(filename, new HashMap<>()), filterPatterns);
    }

    @Override
    public List<Page> parse(String filename, List<String> filterPatterns, Map<String, Object> parserOptions) throws Exception {
        return parse(getFileStream(filename, parserOptions), filterPatterns);
    }

    protected InputStream getFileStream(String filename, Map<String, Object> options) throws Exception {

        String path = FileUtils.getFileUri(filename);
        if (path.startsWith("http")) {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if (options.containsKey(USER_AGENT_OPTION)) {
                conn.addRequestProperty("User-Agent", options.get(USER_AGENT_OPTION).toString());
            }

            return conn.getInputStream();
        }

        return new FileInputStream(new File(path));
    }

}
