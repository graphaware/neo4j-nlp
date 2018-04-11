package com.graphaware.nlp.parser.poi;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.parser.AbstractParser;
import com.graphaware.nlp.parser.domain.Page;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@NLPModuleExtension(name = "ga.nlp.parser.word")
public class WordParser extends AbstractParser {

    @Override
    public List<Page> parse(InputStream fs, List<String> filterPatterns) throws Exception {
        List<Page> pages = new ArrayList<>();
        XWPFDocument document = new XWPFDocument(fs);
        int i = 1;
        for (XWPFParagraph xwpfParagraph : document.getParagraphs()) {
            Page page = new Page(i);
            List<XWPFRun> runs = xwpfParagraph.getRuns();
            StringBuilder sb = new StringBuilder();
            for (XWPFRun run : runs) {
                if (null != run.getText(0)) {
                    sb.append(run.getText(0));
                }
            }
            page.getParagraphs().add(sb.toString());
            if (!sb.toString().equals("")) {
                pages.add(page);
                ++i;
            }
        }

        return pages;
    }
}
