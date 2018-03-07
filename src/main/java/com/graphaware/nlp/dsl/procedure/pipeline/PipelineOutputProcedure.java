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
package com.graphaware.nlp.dsl.procedure.pipeline;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.NodeResult;
import java.util.Map;
import java.util.stream.Stream;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class PipelineOutputProcedure extends AbstractDSL {
    @Procedure(name = "ga.nlp.pipeline.output.create", mode = Mode.WRITE)
    @Description("Create a Pipeline input")
    public Stream<NodeResult> create(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname, 
            @Name(value = "parameters", defaultValue = "" ) Map<String, Object> parameters) {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.output.list", mode = Mode.READ)
    @Description("List Pipelines")
    public Stream<NodeResult> list() {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.output.get", mode = Mode.READ)
    @Description("Get Pipeline info")
    public Stream<NodeResult> get() {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.output.update", mode = Mode.WRITE)
    @Description("Update a Pipeline update")
    public Stream<NodeResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname, 
            @Name(value = "parameters", defaultValue = "" ) Map<String, Object> parameters) {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.output.delete", mode = Mode.WRITE)
    @Description("Delete a Pipeline")
    public Stream<NodeResult> delete(@Name(value = "name") String name) {
        return null;
    }    
    
}
