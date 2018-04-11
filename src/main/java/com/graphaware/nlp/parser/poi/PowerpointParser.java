package com.graphaware.nlp.parser.poi;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.parser.AbstractParser;
import com.graphaware.nlp.parser.domain.Page;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@NLPModuleExtension(name = "ga.nlp.parser.powerpoint")
public class PowerpointParser extends AbstractParser {

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
}
