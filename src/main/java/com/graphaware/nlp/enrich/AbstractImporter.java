package com.graphaware.nlp.enrich;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.domain.Tag;

import java.util.List;

import static com.graphaware.nlp.util.TextUtils.removeApices;
import static com.graphaware.nlp.util.TextUtils.removeParenthesis;

public class AbstractImporter {

    protected boolean checkLanguages(boolean filterLang, String sourceLanguage , String conceptLanguage, List<String> outLang) {
        return !filterLang
                || (filterLang && ((outLang != null && !outLang.isEmpty() && outLang.contains(conceptLanguage)) || conceptLanguage.equalsIgnoreCase(sourceLanguage)));
    }

    protected String getCleanedLemma(Tag source) {
        String word = source.getLemma().toLowerCase().replace(" ", "_");
        word = cleanImportedConcept(word);
        return word;
    }

    protected String cleanImportedConcept(String concept) {
        concept = removeApices(concept);
        concept = removeParenthesis(concept);
        return concept;
    }

    protected Tag tryToAnnotate(String parentConcept, String language) {
        Tag annotateTag = NLPManager.getInstance().getTextProcessorsManager().annotateTag(parentConcept, language);
        if (annotateTag == null) {
            annotateTag = new Tag(parentConcept, language);
        }
        return annotateTag;
    }
}
