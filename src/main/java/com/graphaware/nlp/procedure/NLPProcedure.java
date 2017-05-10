/*
 * Copyright (c) 2013-2016 GraphAware
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
package com.graphaware.nlp.procedure;

import java.util.Map;
import org.neo4j.kernel.api.proc.QualifiedName;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureName;

public class NLPProcedure {

    protected static final String PARAMETER_NAME_INPUT = "input";
    protected static final String PARAMETER_NAME_INPUT_OUTPUT = "result";
    
    protected static final String PARAMETER_NAME_TEXT_PROCESSOR = "textProcessor";
    protected static final String PARAMETER_NAME_TEXT_PIPELINE = "pipeline";



    protected static QualifiedName getProcedureName(String... procedureName) {
        String namespace[] = new String[2 + procedureName.length];
        int i = 0;
        namespace[i++] = "ga";
        namespace[i++] = "nlp";

        for (String value : procedureName) {
            namespace[i++] = value;
        }
        return procedureName(namespace);
    }

    protected void checkIsMap(Object object) throws RuntimeException {
        if (!(object instanceof Map)) {
            throw new RuntimeException("Input parameter is not a map");
        }
    }

}
