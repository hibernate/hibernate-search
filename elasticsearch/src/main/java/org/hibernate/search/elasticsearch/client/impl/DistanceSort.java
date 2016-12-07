/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.spatial.Coordinates;

import com.google.gson.JsonObject;

import io.searchbox.core.search.sort.Sort;

/**
 * Implementation of a Distance sort for Jest.
 *
 * @author Guillaume Smet
 */
public class DistanceSort extends Sort {

	private static final String GEO_DISTANCE_FIELD = "_geo_distance";

	private Coordinates center;
	private String fieldName;

	public DistanceSort(String fieldName, Coordinates center, Sorting order) {
		super( GEO_DISTANCE_FIELD, order );
		this.fieldName = fieldName;
		this.center = center;
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObject rootObject = super.toJsonObject();
		JsonObject gsonDistanceFieldObject = rootObject.getAsJsonObject( GEO_DISTANCE_FIELD );
		JsonBuilder.object( gsonDistanceFieldObject )
				.add( fieldName, JsonBuilder.object()
						.addProperty( "lat", center.getLatitude() )
						.addProperty( "lon", center.getLongitude() )
						.build()
				)
				.addProperty( "unit", "km" )
				.addProperty( "distance_type", "arc" );
		return rootObject;
	}

}
