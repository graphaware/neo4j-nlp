package com.graphaware.nlp.parser.vtt;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.extension.AbstractExtension;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NLPModuleExtension(name = "ga.nlp.parser.vtt")
public class VTTParser extends AbstractExtension {

    public List<TranscriptElement> parse(String file) throws Exception {
        List<String> lines = Files.lines(Paths.get(file)).collect(Collectors.toList());

        return processTranscript(lines);
    }

    private List<TranscriptElement> processTranscript(List<String> lines) {
        String currentStartTime = null;
        String currentEndTime = null;
        StringBuilder sb = new StringBuilder();
        List<TranscriptElement> elements = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("-->")) {
                String[] split = line.split("-->");
                if (currentStartTime == null || sb.toString().equalsIgnoreCase("")) {
                    currentStartTime = split[0].trim();
                }
                currentEndTime = split[1].trim();
            } else {
                if (currentStartTime == null) {
                    continue;
                }

                if (!sb.toString().equalsIgnoreCase("") && !sb.toString().endsWith(" ") && !line.startsWith(" ")) {
                    sb.append(" ");
                }
                sb.append(line.trim());
                if (line.endsWith(".")) {
                    elements.add(new TranscriptElement(currentStartTime, currentEndTime, sb.toString().trim()));
                    sb = new StringBuilder();
                }
            }
        }

        return elements;
    }

}
