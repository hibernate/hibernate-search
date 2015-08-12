/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.spatial.SpatialFieldBridgeByHash;
import org.hibernate.search.spatial.SpatialFieldBridgeByRange;
import org.hibernate.search.spatial.impl.SpatialQueryBuilderFromCoordinates;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
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
		final DocumentBuilderIndexedEntity documentBuilder = Helper.getDocumentBuilder( queryContext );
		//FIXME that will have to change probably but today, if someone uses latitude / longitude
		//      we use boolean style spatial queries
		//      and on coordinates field, use spatial hash query
		// FIXME in the future we will likely react to some state stored in SpatialFieldBridge (for the indexing strategy)
		String coordinatesField = spatialContext.getCoordinatesField();
		FieldBridge fieldBridge = documentBuilder.getBridge( coordinatesField );
		if ( fieldBridge instanceof SpatialFieldBridgeByHash ) {
			return SpatialQueryBuilderFromCoordinates.buildSpatialQueryByHash(
					spatialContext.getCoordinates(),
					spatialContext.getRadiusDistance(), // always in KM so far, no need to convert
					coordinatesField );
		}
		else if ( fieldBridge instanceof SpatialFieldBridgeByRange ) {
			return SpatialQueryBuilderFromCoordinates.buildSpatialQueryByRange(
					spatialContext.getCoordinates(),
					spatialContext.getRadiusDistance(), //always in KM so far, no need to convert
					coordinatesField
			);
		}
		else {
			throw LOG.targetedFieldNotSpatial( queryContext.getEntityType().getName(), coordinatesField );
		}
	}
}
