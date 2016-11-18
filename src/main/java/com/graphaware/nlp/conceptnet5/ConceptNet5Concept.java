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
package com.graphaware.nlp.conceptnet5;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptNet5Concept {

    @JsonProperty("end")
    private IdAwareElement end;
    //private List<String> features;
    //private String id;
    //private String license;
    @JsonProperty("rel")
    private IdAwareElement rel;
    //private String source_uri;
    //private List<String> sources;
    @JsonProperty("start")
    private IdAwareElement start;
    //private String surfaceEnd;
    //private String surfaceStart;
    //private String surfaceText;
    //private String uri;
    private float weight;
//  public String getContext()
//  {
//    return context;
//  }
//  public void setContext(String context)
//  {
//    this.context = context;
//  }
//  public String getDataset()
//  {
//    return dataset;
//  }
//  public void setDataset(String dataset)
//  {
//    this.dataset = dataset;
//  }

    public String getEnd() {
        return end.id;
    }
//  public List<String> getFeatures()
//  {
//    return features;
//  }
//  public void setFeatures(List<String> features)
//  {
//    this.features = features;
//  }
//  public String getId()
//  {
//    return id;
//  }
//  public void setId(String id)
//  {
//    this.id = id;
//  }
//  public String getLicense()
//  {
//    return license;
//  }
//  public void setLicense(String license)
//  {
//    this.license = license;
//  }

    public String getRel() {
        return rel.id;
    }

//  public String getSource_uri()
//  {
//    return source_uri;
//  }
//  public void setSource_uri(String source_uri)
//  {
//    this.source_uri = source_uri;
//  }
//  public List<String> getSources()
//  {
//    return sources;
//  }
//  public void setSources(List<String> sources)
//  {
//    this.sources = sources;
//  }
    public String getStart() {
        return start.id;
    }

//  public String getSurfaceEnd()
//  {
//    return surfaceEnd;
//  }
//  public void setSurfaceEnd(String surfaceEnd)
//  {
//    this.surfaceEnd = surfaceEnd;
//  }
//  public String getSurfaceStart()
//  {
//    return surfaceStart;
//  }
//  public void setSurfaceStart(String surfaceStart)
//  {
//    this.surfaceStart = surfaceStart;
//  }
//  public String getSurfaceText()
//  {
//    return surfaceText;
//  }
//  public void setSurfaceText(String surfaceText)
//  {
//    this.surfaceText = surfaceText;
//  }
//  public String getUri()
//  {
//    return uri;
//  }
//  public void setUri(String uri)
//  {
//    this.uri = uri;
//  }
    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class IdAwareElement {

        public IdAwareElement() {
        }

        @JsonProperty("@id")
        protected String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
