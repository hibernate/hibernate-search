/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.SpatialMatchingContext;
import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.query.dsl.WithinContext;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ConnectedSpatialMatchingContext implements SpatialMatchingContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final SpatialQueryContext spatialContext;

	public ConnectedSpatialMatchingContext(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer, SpatialQueryContext spatialContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.spatialContext = spatialContext;
	}

	@Override
	public WithinContext within(double distance, Unit unit) {
		spatialContext.setRadius( distance, unit );
		return new ConnectedWithinContext( this );
	}

	private static final class ConnectedWithinContext implements WithinContext, WithinContext.LongitudeContext {
		private final ConnectedSpatialMatchingContext mother;
		private double latitude;

		public ConnectedWithinContext(ConnectedSpatialMatchingContext mother) {
			this.mother = mother;
		}

		@Override
		public SpatialTermination ofCoordinates(Coordinates coordinates) {
			mother.spatialContext.setCoordinates( coordinates );
			return new ConnectedSpatialQueryBuilder( mother.spatialContext, mother.queryCustomizer, mother.queryContext );
		}

		@Override
		public LongitudeContext ofLatitude(double latitude) {
			this.latitude = latitude;
			return this;
		}

		@Override
		public SpatialTermination andLongitude(double longitude) {
			mother.spatialContext.setCoordinates( Point.fromDegrees( latitude, longitude ) );
			return new ConnectedSpatialQueryBuilder( mother.spatialContext, mother.queryCustomizer, mother.queryContext );
		}
	}
}
