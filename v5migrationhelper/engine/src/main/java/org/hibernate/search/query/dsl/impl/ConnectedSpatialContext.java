/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.SpatialContext;
import org.hibernate.search.query.dsl.SpatialMatchingContext;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.query.dsl.WithinContext;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedSpatialContext implements SpatialContext {

	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final SpatialQueryContext spatialContext;

	public ConnectedSpatialContext(QueryBuildingContext context) {
		this.queryContext = context;
		this.queryCustomizer = new QueryCustomizer();
		//today we only do constant score for spatial queries
		queryCustomizer.withConstantScore();
		spatialContext = new SpatialQueryContext();
	}

	@Override
	public SpatialMatchingContext onField(String fieldName) {
		spatialContext.setCoordinatesField( fieldName );
		return new ConnectedSpatialMatchingContext( queryContext, queryCustomizer, spatialContext );
	}

	@Override
	public SpatialContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public SpatialContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public SpatialContext filteredBy(Query filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}

	@Override
	public WithinContext within(double distance, Unit unit) {
		spatialContext.setRadius( distance, unit );
		return new ConnectedWithinContext( this );
	}

	QueryBuildingContext getQueryContext() {
		return queryContext;
	}

	QueryCustomizer getQueryCustomizer() {
		return queryCustomizer;
	}

	SpatialQueryContext getSpatialContext() {
		return spatialContext;
	}
}
