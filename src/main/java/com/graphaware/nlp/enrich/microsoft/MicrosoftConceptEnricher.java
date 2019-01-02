package com.graphaware.nlp.enrich.microsoft;

import com.graphaware.common.util.Pair;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.enrich.AbstractEnricher;
import com.graphaware.nlp.enrich.Enricher;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MicrosoftConceptEnricher extends AbstractEnricher implements Enricher {

    public static final String ENRICHER_NAME = "MICROSOFT_CONCEPT";
    private static final String ALIAS_NAME = "microsoft";
    private final TextProcessorsManager textProcessorsManager;


    private MicrosoftConteptImporter microsoftConteptImporter;


    public MicrosoftConceptEnricher(
            GraphDatabaseService database,
            PersistenceRegistry persistenceRegistry,
            TextProcessorsManager textProcessorsManager) {
        super(database, persistenceRegistry);
        this.textProcessorsManager = textProcessorsManager;

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            //
        }
    }

    @Override
    public String getName() {
        return ENRICHER_NAME;
    }

    @Override
    public String getAlias() {
        return ALIAS_NAME;
    }

    @Override
    public Node importConcept(ConceptRequest request) {
        List<Tag> conceptTags = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();
        Pair<Iterator<Node>, Node> pair = getTagsIteratorFromRequest(request);
        Iterator<Node> tagsIterator = pair.first();
        Node tagToBeAnnotated = pair.second();
        while (tagsIterator.hasNext()) {
            Tag tag = (Tag) getPersister(Tag.class).fromNode(tagsIterator.next());
            tags.add(tag);
        }

        tags.forEach(tag -> {
            getImporter().importHierarchy(tag, 20, ENRICHER_NAME).forEach(conceptTag -> {
                conceptTag.getParents().forEach(parent -> {conceptTag.addParent(parent);});
                conceptTags.add(conceptTag);
            });
            conceptTags.add(tag);
        });

        conceptTags.forEach((newTag) -> {
            if (newTag != null) {
                getPersister(Tag.class).getOrCreate(newTag, newTag.getId(), String.valueOf(System.currentTimeMillis()));
            }
        });

        return tagToBeAnnotated;
    }


    private MicrosoftConteptImporter getImporter() {
        if (microsoftConteptImporter == null) {
            this.microsoftConteptImporter = new MicrosoftConteptImporter();
        }
        return microsoftConteptImporter;
    }


}
