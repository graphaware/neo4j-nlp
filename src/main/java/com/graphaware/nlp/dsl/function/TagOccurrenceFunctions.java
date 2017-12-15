/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.dsl.function;

import com.graphaware.nlp.persistence.constants.Relationships;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class TagOccurrenceFunctions {

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
