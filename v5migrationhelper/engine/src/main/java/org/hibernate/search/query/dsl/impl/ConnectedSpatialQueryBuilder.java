/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.backend.lucene.search.spi.LuceneMigrationUtils;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SpatialWithinPredicateOptionsStep;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.spatial.Coordinates;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedSpatialQueryBuilder implements SpatialTermination {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final SpatialQueryContext spatialContext;

	public ConnectedSpatialQueryBuilder(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer,
			SpatialQueryContext spatialContext) {
		this.queryContext = queryContext;
		this.spatialContext = spatialContext;
		this.queryCustomizer = queryCustomizer;
	}

	@Override
	public Query createQuery() {
		return LuceneMigrationUtils.toLuceneQuery( createPredicate() );
	}

	private SearchPredicate createPredicate() {
		SearchPredicateFactory factory = queryContext.getScope().predicate();

		SpatialWithinPredicateOptionsStep<?> optionsStep = factory.spatial().within()
				.field( spatialContext.getCoordinatesField() )
				.circle( Coordinates.toGeoPoint( spatialContext.getCoordinates() ),
						spatialContext.getRadiusDistance(), DistanceUnit.KILOMETERS );

		queryCustomizer.applyScoreOptions( optionsStep );
		SearchPredicate predicate = optionsStep.toPredicate();
		return queryCustomizer.applyFilter( factory, predicate );
	}

}
