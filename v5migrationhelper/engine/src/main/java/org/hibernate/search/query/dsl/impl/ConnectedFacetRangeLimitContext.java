/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetRangeEndContext;
import org.hibernate.search.query.dsl.FacetRangeLimitContext;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetRangeLimitContext<T> implements FacetRangeLimitContext<T> {
	private final FacetBuildingContext context;

	public ConnectedFacetRangeLimitContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public FacetRangeLimitContext<T> excludeLimit() {
		context.setIncludeRangeStart( false );
		return this;
	}

	@Override
	public FacetRangeEndContext<T> to(T upperLimit) {
		context.setRangeEnd( upperLimit );
		return new ConnectedFacetRangeEndContext( context );
	}

}

