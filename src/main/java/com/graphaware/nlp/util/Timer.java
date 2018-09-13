package com.graphaware.nlp.util;

import com.graphaware.common.log.LoggerFactory;
import org.neo4j.logging.Log;

public class Timer {

    private static final Log LOG = LoggerFactory.getLogger(Timer.class);

    private final String timerName;

    private final long startTime;

    private long lastLap;

    public Timer(long startTime, String name) {
        this.startTime = startTime;
        this.lastLap = startTime;
        this.timerName = name;
    }

    public static Timer start(String name) {
        return new Timer(System.currentTimeMillis(), name);
    }

    public static Timer start() {
        return start("");
    }

    public void lap(String tag) {
        long now = System.currentTimeMillis();
        long sinceBeginning = now - startTime;
        long sinceLastLap = now - lastLap;
        LOG.debug("Timer:: " + timerName + " (" + tag + ") - Since beginning : " + sinceBeginning + " - Since last lap : " + sinceLastLap);
        System.out.println("Timer:: " + timerName + " (" + tag + ") - Since beginning : " + sinceBeginning + " - Since last lap : " + sinceLastLap);
        lastLap = now;
    }

    public void lap() {
        lap("NONE");
    }

    public void stop() {
        lap("STOP");
    }

}
