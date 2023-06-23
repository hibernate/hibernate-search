/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetRangeAboveContext;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetRangeAboveContext<T> extends ConnectedFacetParameterContext
		implements FacetRangeAboveContext<T> {
	private final FacetBuildingContext context;

	public ConnectedFacetRangeAboveContext(FacetBuildingContext context) {
		super( context );
		this.context = context;
	}

	@Override
	public FacetRangeAboveContext<T> excludeLimit() {
		context.setIncludeRangeStart( false );
		return this;
	}

	@Override
	public FacetingRequest createFacetingRequest() {
		context.makeRange();
		return context.getFacetingRequest();
	}
}

