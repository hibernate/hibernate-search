/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.DiscreteFacetContext;
import org.hibernate.search.query.dsl.FacetContinuationContext;
import org.hibernate.search.query.dsl.FacetRangeAboveBelowContext;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetContinuationContext implements FacetContinuationContext {
	private final FacetBuildingContext context;

	public ConnectedFacetContinuationContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public <T> FacetRangeAboveBelowContext<T> range() {
		context.setRangeQuery( true );
		return new ConnectedFacetRangeAboveBelowContext<T>( context );
	}

	@Override
	public DiscreteFacetContext discrete() {
		return new ConnectedDiscreteFacetContext( context );
	}
}

