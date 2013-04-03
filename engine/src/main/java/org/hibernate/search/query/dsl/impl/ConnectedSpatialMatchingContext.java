/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
	private final ConnectedQueryBuilder queryBuilder;

	public ConnectedSpatialMatchingContext(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer, SpatialQueryContext spatialContext, ConnectedQueryBuilder queryBuilder) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.spatialContext = spatialContext;
		this.queryBuilder = queryBuilder;
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
			return new ConnectedSpatialQueryBuilder( mother.spatialContext, mother.queryCustomizer, mother.queryContext, mother.queryBuilder );
		}

		@Override
		public LongitudeContext ofLatitude(double latitude) {
			this.latitude = latitude;
			return this;
		}

		@Override
		public SpatialTermination andLongitude(double longitude) {
			mother.spatialContext.setCoordinates( Point.fromDegrees( latitude, longitude ) );
			return new ConnectedSpatialQueryBuilder( mother.spatialContext, mother.queryCustomizer, mother.queryContext, mother.queryBuilder );
		}
	}
}
