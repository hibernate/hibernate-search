/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetRangeAboveBelowContext;
import org.hibernate.search.query.dsl.FacetRangeAboveContext;
import org.hibernate.search.query.dsl.FacetRangeBelowContinuationContext;
import org.hibernate.search.query.dsl.FacetRangeLimitContext;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetRangeAboveBelowContext<T> implements FacetRangeAboveBelowContext<T> {
	private final FacetBuildingContext context;

	public ConnectedFacetRangeAboveBelowContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public FacetRangeLimitContext<T> from(T rangeStart) {
		context.setRangeStart( rangeStart );
		return new ConnectedFacetRangeLimitContext<T>( context );
	}

	@Override
	public FacetRangeBelowContinuationContext<T> below(T min) {
		context.setRangeStart( null );
		context.setRangeEnd( min );
		return new ConnectedFacetRangeBelowContinuationContext( context );
	}

	@Override
	public FacetRangeAboveContext<T> above(T max) {
		context.setRangeStart( max );
		context.setRangeEnd( null );
		return new ConnectedFacetRangeAboveContext<T>( context );
	}
}

