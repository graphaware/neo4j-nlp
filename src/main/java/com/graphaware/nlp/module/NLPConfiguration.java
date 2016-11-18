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
package com.graphaware.nlp.module;

import com.graphaware.common.policy.InclusionPolicies;
import com.graphaware.common.policy.none.IncludeNoRelationships;
import com.graphaware.runtime.config.BaseTxDrivenModuleConfiguration;
import com.graphaware.runtime.policy.InclusionPoliciesFactory;


/**
 * {@link BaseTxDrivenModuleConfiguration} for {@link com.graphaware.spark.module.UuidModule}.
 */
public class NLPConfiguration extends BaseTxDrivenModuleConfiguration<NLPConfiguration> {

    private static final String DEFAULT_CONCEPTNET_URL = "http://api.conceptnet.io";
    
    private final String conceptNetUrl;

    public NLPConfiguration(InclusionPolicies inclusionPolicies,  long initializeUntil, String conceptNetUrl) {
        super(inclusionPolicies, initializeUntil);
        this.conceptNetUrl = conceptNetUrl;
    }

    public static NLPConfiguration defaultConfiguration() {
        return new NLPConfiguration(InclusionPoliciesFactory
                .allBusiness()
                .with(IncludeNoRelationships.getInstance())
                , ALWAYS, DEFAULT_CONCEPTNET_URL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NLPConfiguration newInstance(InclusionPolicies inclusionPolicies, long initializeUntil) {
        return new NLPConfiguration(inclusionPolicies, initializeUntil, getConceptNetUrl());
    }

    public String getConceptNetUrl() {
        return conceptNetUrl;
    }

    public NLPConfiguration withConceptNetUrl(String conceptNetUrl) {
        return new NLPConfiguration(getInclusionPolicies(), initializeUntil(), conceptNetUrl);
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
