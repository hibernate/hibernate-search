/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.spatial.Coordinates;

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

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> rootMap = super.toMap();
		Map<String, Object> innerMap = (Map<String, Object>) rootMap.get(GEO_DISTANCE_FIELD);

		Map<String, Double> location = new HashMap<String, Double>(3);
		location.put( "lat", center.getLatitude() );
		location.put( "lon", center.getLongitude() );

		innerMap.put( fieldName, location );

		innerMap.put( "unit", "km" );

		return rootMap;
	}

}
