package com.graphaware.nlp.ml.textrank;

import com.graphaware.nlp.domain.Keyword;

import java.util.HashMap;
import java.util.Map;

public class TextRankResult {

    private final Map<String, Keyword> result;

    private final TextRankStatus status;

    private final String reason;

    public static TextRankResult SUCCESS(Map<String, Keyword> result) {
        return new TextRankResult(result, TextRankStatus.SUCCESS, null);
    }

    public static TextRankResult FAILED(String reason) {
        return new TextRankResult(new HashMap<>(), TextRankStatus.FAILED, reason);
    }

    public TextRankResult(Map<String, Keyword> result, TextRankStatus status, String reason) {
        this.result = result;
        this.status = status;
        this.reason = reason;
    }

    public Map<String, Keyword> getResult() {
        return result;
    }

    public TextRankStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    enum TextRankStatus {
        SUCCESS,
        FAILED
    }
}
