package com.graphaware.nlp.concurrent;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.writer.service.QueueBackedScheduledService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;

import java.util.LinkedList;
import java.util.List;

public class QueueBackedPersister extends QueueBackedScheduledService<AnnotatedText> {

    private final NLPManager manager = NLPManager.getInstance();
    private static final Log LOG = LoggerFactory.getLogger(QueueBackedPersister.class);

    @Override
    protected void runOneIteration() {
        persistAnnotatedTexts(getNext());
    }

    private List<AnnotatedText> getNext() {
        List<AnnotatedText> annotatedTexts = new LinkedList<>();
        queue.drainTo(annotatedTexts, 5);

        return annotatedTexts;
    }

    private void persistAnnotatedTexts(List<AnnotatedText> annotatedTexts) {
        if (annotatedTexts.size() < 1) {return;}
        try (Transaction tx = manager.getDatabase().beginTx()) {
            for (AnnotatedText annotatedText : annotatedTexts) {
                manager.persistAnnotatedText(annotatedText, annotatedText.getId(), String.valueOf(System.currentTimeMillis()));
            }
            tx.success();
        }
    }

    @Override
    protected boolean offer(AnnotatedText futureTask) {
        return super.offer(futureTask);
    }
}
