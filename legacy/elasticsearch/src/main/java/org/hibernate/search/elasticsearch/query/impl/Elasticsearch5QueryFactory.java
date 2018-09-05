/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.spatial.Coordinates;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch5QueryFactory implements ElasticsearchQueryFactory {

	@Override
	public JsonObject createSpatialDistanceScript(Coordinates center, String spatialFieldName) {
		return JsonBuilder.object()
				.add( "params",
						JsonBuilder.object()
								.addProperty( "lat", center.getLatitude() )
								.addProperty( "lon", center.getLongitude() )
				)
				// We multiply by 0.001 to Convert from meters to kilometers
				.addProperty(
						"inline",
						"doc['" + spatialFieldName + "'].value !== null ?"
								+ " doc['" + spatialFieldName + "'].arcDistance(params.lat,params.lon)*0.001"
								+ " : null" )
				.addProperty( "lang", "painless" )
				.build();
	}

}
