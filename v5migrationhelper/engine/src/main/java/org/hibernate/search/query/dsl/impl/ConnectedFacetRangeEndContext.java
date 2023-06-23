/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetRangeAboveContext;
import org.hibernate.search.query.dsl.FacetRangeEndContext;
import org.hibernate.search.query.dsl.FacetRangeLimitContext;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetRangeEndContext<T> extends ConnectedFacetParameterContext
		implements FacetRangeEndContext<T> {
	private final FacetBuildingContext context;

	public ConnectedFacetRangeEndContext(FacetBuildingContext context) {
		super( context );
		this.context = context;
	}

	@Override
	public FacetRangeEndContext<T> excludeLimit() {
		context.setIncludeRangeEnd( false );
		return this;
	}

	@Override
	public FacetRangeAboveContext<T> above(T max) {
		context.makeRange();
		context.setRangeStart( max );
		context.setRangeEnd( null );
		return new ConnectedFacetRangeAboveContext<T>( context );
	}

	@Override
	public FacetRangeLimitContext<T> from(T rangeStart) {
		context.makeRange();
		context.setRangeStart( rangeStart );
		return new ConnectedFacetRangeLimitContext<T>( context );
	}

	@Override
	public FacetingRequest createFacetingRequest() {
		context.makeRange();
		return context.getFacetingRequest();
	}
}

