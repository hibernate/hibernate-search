/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.programmatic;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LatitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * A binder from a type or property to a {@link GeoPoint} field, representing a point on earth.
 * <p>
 * These fields allow spatial predicates such as "within" (is the point within a circle, a bounding box, ...),
 * sorts by distance to another point, ...
 *
 * @see GeoPointBinding
 * @see #create()
 * @see #latitude()
 * @see #longitude()
 */
public interface GeoPointBinder
		extends TypeBinder, PropertyBinder {

	/**
	 * @param fieldName The name of the {@link GeoPoint} field.
	 * If used on a property, this defaults to the name of that property.
	 * Otherwise, the name must be defined explicitly.
	 * @return {@code this}, for method chaining.
	 */
	GeoPointBinder fieldName(String fieldName);

	/**
	 * @param projectable Whether projections are enabled for the {@link GeoPoint} field.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#projectable()
	 * @see Projectable
	 */
	GeoPointBinder projectable(Projectable projectable);

	/**
	 * @param sortable Whether the {@link GeoPoint} field should be sortable by distance.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#sortable()
	 * @see Sortable
	 */
	GeoPointBinder sortable(Sortable sortable);

	/**
	 * @param markerSet The name of the "marker set".
	 * This is used to discriminate between multiple pairs of latitude/longitude markers:
	 * {@link LatitudeLongitudeMarkerBinder#markerSet(String) assign a marker set when building each marker},
	 * then select the marker set here.
	 * @return {@code this}, for method chaining.
	 */
	GeoPointBinder markerSet(String markerSet);

	/**
	 * @return A {@link GeoPointBinder}.
	 */
	static GeoPointBinder create() {
		return new org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.GeoPointBridge.Binder();
	}

	/**
	 * @return A {@link MarkerBinder} for the latitude, to be applied on a property.
	 * @see LatitudeLongitudeMarkerBinder
	 */
	static LatitudeLongitudeMarkerBinder latitude() {
		return new LatitudeMarker.Binder();
	}

	/**
	 * @return A {@link MarkerBinder} for the longitude, to be applied on a property.
	 * @see LatitudeLongitudeMarkerBinder
	 */
	static LatitudeLongitudeMarkerBinder longitude() {
		return new LongitudeMarker.Binder();
	}

}
