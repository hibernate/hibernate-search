/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.programmatic;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;

/**
 * A binder for markers that mark a property as the latitude or longitude
 * for a {@link GeoPointBinder GeoPoint bridge}.
 *
 * @see Latitude
 * @see Longitude
 * @see GeoPointBinder#latitude()
 * @see GeoPointBinder#longitude()
 */
public interface LatitudeLongitudeMarkerBinder extends MarkerBinder {

	/**
	 * @param markerSet The name of the "marker set".
	 * This is used to discriminate between multiple pairs of latitude/longitude markers.
	 * @return {@code this}, for method chaining.
	 * @see GeoPointBinder#markerSet(String)
	 */
	LatitudeLongitudeMarkerBinder markerSet(String markerSet);

}
