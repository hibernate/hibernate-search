package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.DiscreteFacetContext;
import org.hibernate.search.query.dsl.FacetParameterContext;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.query.facet.FacetSortOrder;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedDiscreteFacetContext implements DiscreteFacetContext {
	private final FacetBuildingContext context;

	public ConnectedDiscreteFacetContext(FacetBuildingContext context) {
		this.context = context;
	}

	public FacetParameterContext orderedBy(FacetSortOrder sort) {
		context.setSort( sort );
		return new ConnectedFacetParameterContext( context );
	}

	public FacetParameterContext includeZeroCounts(boolean zeroCounts) {
		context.setIncludeZeroCount( zeroCounts );
		return new ConnectedFacetParameterContext( context );
	}

	public FacetParameterContext maxFacetCount(int maxFacetCount) {
		context.setMaxFacetCount( maxFacetCount );
		return new ConnectedFacetParameterContext( context );
	}

	public FacetingRequest createFacetRequest() {
		return context.getFacetRequest();
	}
}


