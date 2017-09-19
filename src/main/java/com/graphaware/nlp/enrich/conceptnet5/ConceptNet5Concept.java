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
package com.graphaware.nlp.enrich.conceptnet5;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptNet5Concept {

    @JsonProperty("end")
    private TermElement end;
    private String dataset;
    private String license;
    @JsonProperty("rel")
    private RelElement rel;
    @JsonProperty("start")
    private TermElement start;
    private String surfaceText;
    private float weight;

    public String getEnd() {
        return end.label;
    }
    
    public String getEndLanguage() {
        return end.language;
    }
    
    public String getRel() {
        return rel.label;
    }

    public String getStart() {
        return start.label;
    }
    
    public String getStartLanguage() {
        return start.language;
    }

    public float getWeight() {
        return weight;
    }

    public String getDataset() {
        return dataset;
    }

    public String getSurfaceText() {
        return surfaceText;
    }

    public String getLicense() {
        return license;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    class TermElement {

        public TermElement() {
        }

        @JsonProperty("@id")
        protected String id;

        protected String label;

        protected String language;

        @JsonProperty("sense_label")
        protected String senseLabel;

        protected String term;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getSenseLabel() {
            return senseLabel;
        }

        public void setSenseLabel(String senseLabel) {
            this.senseLabel = senseLabel;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class RelElement {

        public RelElement() {
        }
        @JsonProperty("@id")
        protected String id;
        protected String label;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
        
    }
}
