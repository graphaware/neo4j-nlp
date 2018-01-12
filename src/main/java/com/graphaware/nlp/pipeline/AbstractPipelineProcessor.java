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
package com.graphaware.nlp.pipeline;

public abstract class AbstractPipelineProcessor<C extends PipelineConfiguration> {
    
    private final String name;
    private final PipelineConfiguration configuration;
    private boolean valid;

    public AbstractPipelineProcessor(String name, PipelineConfiguration configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    public String getName() {
        return name;
    }
    
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public PipelineConfiguration getConfiguration() {
        return configuration;
    }
    
}
