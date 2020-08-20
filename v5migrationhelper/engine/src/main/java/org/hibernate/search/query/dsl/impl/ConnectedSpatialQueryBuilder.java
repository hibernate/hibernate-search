/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;

import org.hibernate.search.query.dsl.SpatialTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedSpatialQueryBuilder implements SpatialTermination {
	private final SpatialQueryContext spatialContext;
	private final QueryCustomizer queryCustomizer;
	private final QueryBuildingContext queryContext;

	public ConnectedSpatialQueryBuilder(SpatialQueryContext spatialContext, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.spatialContext = spatialContext;
		this.queryCustomizer = queryCustomizer;
		this.queryContext = queryContext;
	}

	@Override
	public Query createQuery() {
		return queryCustomizer.setWrappedQuery( createSpatialQuery() ).createQuery();
	}

	private Query createSpatialQuery() {
		throw new UnsupportedOperationException( "To be implemented through the Search 6 DSL" );
	}
}
