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

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.spatial.SpatialFieldBridge;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spatial.impl.Rectangle;
import org.hibernate.search.spatial.impl.SpatialQueryBuilderFromPoint;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ConnectedSpatialQueryBuilder implements SpatialTermination {
	private static final Log LOG = LoggerFactory.make();

	private final SpatialQueryContext spatialContext;
	private final QueryCustomizer queryCustomizer;
	private final QueryBuildingContext queryContext;
	private final ConnectedQueryBuilder queryBuilder;

	public ConnectedSpatialQueryBuilder(SpatialQueryContext spatialContext, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext, ConnectedQueryBuilder queryBuilder) {
		this.spatialContext = spatialContext;
		this.queryCustomizer = queryCustomizer;
		this.queryContext = queryContext;
		this.queryBuilder = queryBuilder;
	}

	@Override
	public Query createQuery() {
		return queryCustomizer.setWrappedQuery( createSpatialQuery() ).createQuery();
	}

	private Query createSpatialQuery() {
		final DocumentBuilderIndexedEntity<?> documentBuilder = Helper.getDocumentBuilder( queryContext );
		//FIXME that will have to change probably but today, if someone uses latitude / longitude
		//      we use boolean style spatial queries
		//      and on coordinates field, use grid query
		// FIXME in the future we will likely react to some state stored in SpatialFieldBridge (for the indexing strategy)
		String coordinatesField = spatialContext.getCoordinatesField();
		if ( coordinatesField == null ) {
			Point center = Point.fromDegrees(
					spatialContext.getCoordinates().getLatitude(),
					spatialContext.getCoordinates().getLongitude()
			);
			Rectangle boundingBox = Rectangle.fromBoundingCircle(center, spatialContext.getRadiusDistance() /* no conversion needed as we have one unit */ );

			org.apache.lucene.search.Query query = queryBuilder.bool()
				.must(
					queryBuilder.range()
							.onField( spatialContext.getLatitudeField() )
							.from( boundingBox.getLowerLeft().getLatitude() )
							.to( boundingBox.getUpperRight().getLatitude() )
							.createQuery()
				)
				.must(
					queryBuilder.range()
							.onField( spatialContext.getLongitudeField() )
							.from( boundingBox.getLowerLeft().getLongitude() )
							.to( boundingBox.getUpperRight().getLongitude() )
							.createQuery()
				)
				.createQuery();
			org.apache.lucene.search.Query filteredQuery = new ConstantScoreQuery(
					SpatialQueryBuilderFromPoint.buildDistanceFilter(
							new QueryWrapperFilter( query ),
							center,
							spatialContext.getRadiusDistance(),
							spatialContext.getLatitudeField(),
							spatialContext.getLongitudeField()
					)
			);
			return filteredQuery;
		}
		else {
			FieldBridge fieldBridge = documentBuilder.getBridge( coordinatesField );
			if ( fieldBridge instanceof SpatialFieldBridge ) {
				return SpatialQueryBuilderFromPoint.buildSpatialQuery(
						Point.fromDegrees(
								spatialContext.getCoordinates().getLatitude(),
								spatialContext.getCoordinates().getLongitude()
						),
						spatialContext.getRadiusDistance(), //always in KM so far, no need to convert
						coordinatesField
				);
			}
			else {
				throw LOG.targetedFieldNotSpatial( queryContext.getEntityType().getName(), coordinatesField );
			}
		}
	}
}
