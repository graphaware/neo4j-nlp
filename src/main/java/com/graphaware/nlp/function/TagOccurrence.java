package com.graphaware.nlp.function;

import com.graphaware.nlp.persistence.Relationships;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class TagOccurrence {

    @Context
    public GraphDatabaseService database;

    @UserFunction("ga.nlp.tagForOccurrence")
    @Description("Returns the Tag node associated to a given TagOccurrence")
    public Node getTagForOccurrence(@Name("occurrence") Node occurrence) {
        try {
            return occurrence.getSingleRelationship(Relationships.TAG_OCCURRENCE_TAG, Direction.OUTGOING).getEndNode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
