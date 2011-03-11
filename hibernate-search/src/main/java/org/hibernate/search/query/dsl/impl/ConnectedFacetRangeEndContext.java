package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetRangeEndContext;
import org.hibernate.search.query.dsl.FacetRangeLimitContext;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetRangeEndContext<T> implements FacetRangeEndContext<T> {
	private final FacetBuildingContext context;

	public ConnectedFacetRangeEndContext(FacetBuildingContext context) {
		this.context = context;
	}

	public FacetRangeEndContext<T> excludeLimit() {
		context.setIncludeRangeEnd( false );
		return this;
	}

	public FacetRangeLimitContext<T> from(T rangeStart) {
		context.makeRange();
		context.setRangeStart( rangeStart );
		return new ConnectedFacetRangeLimitContext<T>( context );
	}

	public FacetingRequest createFacetRequest() {
		context.makeRange();
		return context.getFacetRequest();
	}
}


