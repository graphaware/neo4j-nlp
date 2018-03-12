package com.graphaware.nlp.concurrent;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.request.AnnotationRequest;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.extension.NLPExtension;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.concurrent.Executors;

@NLPModuleExtension(name = "concurrent_annotator")
public class ConcurrentAnnotator implements NLPExtension{

    private final QueueBackedPersister queueBackedPersister = new QueueBackedPersister();
    private final NLPManager manager = NLPManager.getInstance();

    @Override
    public void registerEventListeners(EventDispatcher eventDispatcher) {
    }

    @Override
    public void postLoaded() {
        queueBackedPersister.start();
    }

    public void annotateInBatch(AnnotationRequest request) {
        MultiThreader multiThreader = new MultiThreader(Executors.newFixedThreadPool(4));
        GraphDatabaseService db = manager.getDatabase();
        try (Transaction tx = db.beginTx()) {
            Result result = db.execute(request.getQuery());
            while (result.hasNext()) {
                Node n = (Node) result.next().get("n");
                AnnotationRequest newRequest = new AnnotationRequest(n.getProperty("text").toString(), String.valueOf(n.getId()), null, request.getTextProcessor(), request.getPipeline(), request.isForce(), request.shouldCheckLanguage());
                multiThreader.add(new Runnable() {
                    @Override
                    public void run() {
                        AnnotatedText annotatedText = manager.annotateText(newRequest.getText(), newRequest.getId(), newRequest.getTextProcessor(), newRequest.getPipeline(), newRequest.isForce(), newRequest.shouldCheckLanguage());
                        annotatedText.setId(String.valueOf(n.getId()));
                        queueBackedPersister.offer(annotatedText);
                    }
                });
            }
            tx.success();
        }

        multiThreader.waitUntilDone();
    }

}
