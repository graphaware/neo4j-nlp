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
package com.graphaware.nlp.module;

import com.graphaware.common.policy.inclusion.InclusionPolicies;
import com.graphaware.common.policy.inclusion.none.IncludeNoRelationships;
import com.graphaware.runtime.config.BaseTxDrivenModuleConfiguration;
import com.graphaware.runtime.policy.InclusionPoliciesFactory;


public class NLPConfiguration extends BaseTxDrivenModuleConfiguration<NLPConfiguration> {

    private static final String DEFAULT_CONCEPTNET_URL = "http://api.conceptnet.io";
    //@todo spark settings should go to EE module
    private static final String DEFAULT_SPARK_REST_URL = "http://localhost:8082";
    
    private final String conceptNetUrl;
    private final String sparkRestUrl;

    public NLPConfiguration(InclusionPolicies inclusionPolicies,  long initializeUntil, String conceptNetUrl, String sparkRestUrl) {
        super(inclusionPolicies, initializeUntil);
        this.conceptNetUrl = conceptNetUrl;
        this.sparkRestUrl = sparkRestUrl;
    }

    public static NLPConfiguration defaultConfiguration() {
        return new NLPConfiguration(InclusionPoliciesFactory
                .allBusiness()
                .with(IncludeNoRelationships.getInstance()), 
                ALWAYS, 
                DEFAULT_CONCEPTNET_URL, 
                DEFAULT_SPARK_REST_URL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NLPConfiguration newInstance(InclusionPolicies inclusionPolicies, long initializeUntil) {
        return new NLPConfiguration(inclusionPolicies, initializeUntil, getConceptNetUrl(), getSparkRestUrl());
    }

    public String getConceptNetUrl() {
        return conceptNetUrl;
    }

    public String getSparkRestUrl() {
        return sparkRestUrl;
    }
    
    public NLPConfiguration withConceptNetUrl(String conceptNetUrl) {
        return new NLPConfiguration(getInclusionPolicies(), initializeUntil(), conceptNetUrl, getSparkRestUrl());
    }
    
    public NLPConfiguration withSparkRestUrl(String sparkRestUrl) {
        return new NLPConfiguration(getInclusionPolicies(), initializeUntil(), getConceptNetUrl(), sparkRestUrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        NLPConfiguration that = (NLPConfiguration) o;

        if (!conceptNetUrl.equals(that.conceptNetUrl)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + conceptNetUrl.hashCode();
        return result;
    }
}
