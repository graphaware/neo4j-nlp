package com.graphaware.nlp.parser.vtt;

public class TranscriptElement {

    public String startTime;

    public String endTime;

    public String text;

    public TranscriptElement(String startTime, String endTime, String text) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getText() {
        return text;
    }
}
