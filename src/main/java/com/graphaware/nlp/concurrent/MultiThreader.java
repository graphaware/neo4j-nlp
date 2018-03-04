package com.graphaware.nlp.concurrent;

import com.graphaware.common.log.LoggerFactory;
import org.neo4j.logging.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MultiThreader {

    private final Log LOG = LoggerFactory.getLogger(MultiThreader.class);
    private final ExecutorService executor;
    private final List<Future<?>> futures = new LinkedList<>();

    public MultiThreader(ExecutorService executor) {
        this.executor = executor;
    }

    public void add(Runnable runnable) {
        futures.add(executor.submit(runnable));
    }

    public void waitUntilDone() {
        for (Future<?> future : futures) {
            try {
                future.get(5, TimeUnit.MINUTES);
            } catch (Exception e) {
                LOG.warn("Annotation took too long, terminating.");
                throw new RuntimeException(e);
            }
        }
    }
}
