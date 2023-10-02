/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.query.dsl.WithinContext;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;

/**
 * @author Hardy Ferentschik
 */
final class ConnectedWithinContext implements WithinContext, WithinContext.LongitudeContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final SpatialQueryContext spatialContext;
	private double latitude;

	public ConnectedWithinContext(ConnectedSpatialContext mother) {
		queryContext = mother.getQueryContext();
		queryCustomizer = mother.getQueryCustomizer();
		spatialContext = mother.getSpatialContext();
	}

	public ConnectedWithinContext(ConnectedSpatialMatchingContext mother) {
		queryContext = mother.getQueryContext();
		queryCustomizer = mother.getQueryCustomizer();
		spatialContext = mother.getSpatialContext();
	}

	@Override
	public SpatialTermination ofCoordinates(Coordinates coordinates) {
		spatialContext.setCoordinates( coordinates );
		return new ConnectedSpatialQueryBuilder(
				queryContext,
				queryCustomizer,
				spatialContext
		);
	}

	@Override
	public LongitudeContext ofLatitude(double latitude) {
		this.latitude = latitude;
		return this;
	}

	@Override
	public SpatialTermination andLongitude(double longitude) {
		spatialContext.setCoordinates( Point.fromDegrees( latitude, longitude ) );
		return new ConnectedSpatialQueryBuilder(
				queryContext,
				queryCustomizer,
				spatialContext
		);
	}
}

