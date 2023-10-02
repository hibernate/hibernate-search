/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetParameterContext;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetParameterContext implements FacetParameterContext {
	private final FacetBuildingContext context;

	public ConnectedFacetParameterContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public FacetParameterContext orderedBy(FacetSortOrder sort) {
		context.setSort( sort );
		return this;
	}

	@Override
	public FacetParameterContext includeZeroCounts(boolean zeroCounts) {
		context.setIncludeZeroCount( zeroCounts );
		return this;
	}

	@Override
	public FacetParameterContext maxFacetCount(int maxFacetCount) {
		context.setMaxFacetCount( maxFacetCount );
		return this;
	}

	@Override
	public FacetingRequest createFacetingRequest() {
		return context.getFacetingRequest();
	}
}

