package com.graphaware.nlp.configuration;

import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;

public class MigrationHandler {

    private static final Log LOG = LoggerFactory.getLogger(MigrationHandler.class);

    private final GraphDatabaseService database;
    private final DynamicConfiguration configuration;
    private final GraphKeyValueStore graphKeyValueStore;
    private final ObjectMapper mapper = new ObjectMapper();

    public MigrationHandler(GraphDatabaseService database, DynamicConfiguration configuration) {
        this.database = database;
        this.configuration = configuration;
        this.graphKeyValueStore = new GraphKeyValueStore(database);
    }

    public void migrate() {
        try (Transaction tx = database.beginTx()) {
            graphKeyValueStore.getKeys().forEach(k -> {
                if (isPipelineSpecification(k)) {
                    try {
                        doMigrate(k);
                    } catch (Exception e) {
                        LOG.error(e.getMessage());
                    }
                }
            });
            tx.success();
        }
    }

    private void doMigrate(String k) throws Exception {
        String specStr = graphKeyValueStore.get(k).toString();
        if (!specStr.contains("\"@class\"")) {
            PipelineSpecification pipelineSpecification = mapper.readValue(specStr, PipelineSpecification.class);
            graphKeyValueStore.remove(k);
            configuration.storeCustomPipeline(pipelineSpecification);
        }
    }

    private static boolean isPipelineSpecification(String k) {
        return k.startsWith(DynamicConfiguration.STORE_KEY + DynamicConfiguration.PIPELINE_KEY_PREFIX);
    }


}
