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

import org.apache.lucene.search.Query;

import org.hibernate.search.SearchException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.spatial.SpatialFieldBridge;
import org.hibernate.search.spatial.SpatialQueryBuilder;
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
		final DocumentBuilderIndexedEntity<?> documentBuilder = Helper.getDocumentBuilder( queryContext );
		if ( spatialContext.getLatitudeField() != null || spatialContext.getLongitudeField() != null) {
			throw new SearchException( "latitude and longitude fields support is not implemented yet" );
		}
		String coordinatesField = spatialContext.getCoordinatesField();
		FieldBridge fieldBridge = documentBuilder.getBridge( coordinatesField );
		if ( fieldBridge instanceof SpatialFieldBridge ) {
			return SpatialQueryBuilder.buildSpatialQuery(
					spatialContext.getCoordinates().getLatitude(),
					spatialContext.getCoordinates().getLongitude(),
					spatialContext.getRadiusDistance(), //always in KM so far, no need to convert
					coordinatesField
			);
		}
		else {
			throw LOG.targetedFieldNotSpatial( queryContext.getEntityType().getName(), coordinatesField );
		}
	}
}
