/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetRangeLimitContext;
import org.hibernate.search.query.dsl.FacetRangeStartContext;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetRangeStartContext<T> implements FacetRangeStartContext<T> {
	private final FacetBuildingContext context;

	public ConnectedFacetRangeStartContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public FacetRangeLimitContext<T> from(T rangeStart) {
		context.setRangeStart( rangeStart );
		return new ConnectedFacetRangeLimitContext<T>( context );
	}
}

