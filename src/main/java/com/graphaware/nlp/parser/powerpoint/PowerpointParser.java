package com.graphaware.nlp.parser.powerpoint;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.parser.Parser;
import com.graphaware.nlp.parser.domain.Page;
import com.graphaware.nlp.util.FileUtils;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@NLPModuleExtension(name = "ga.nlp.parser.powerpoint")
public class PowerpointParser extends AbstractExtension implements Parser {

    public List<Page> parse(String filename, List<String> filterPatterns) throws Exception {
        return parse(getFileStream(filename), filterPatterns);
    }

    @Override
    public List<Page> parse(InputStream fs, List<String> filterPatterns) throws Exception {
        List<Page> pages = new ArrayList<>();
        XMLSlideShow ppt = new XMLSlideShow(fs);
        ppt.getSlides().forEach(slide -> {
            Page page = new Page(slide.getSlideNumber());
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape) {
                    page.getParagraphs().add(((XSLFTextShape) shape).getText());
                }
            }
            pages.add(page);
        });

        return pages;
    }

    private InputStream getFileStream(String filename) throws Exception {

        String path = FileUtils.getFileUri(filename);
        if (path.startsWith("http")) {
            URL url = new URL(path);
            return url.openStream();
        }

        return new FileInputStream(new File(path));
    }
}
