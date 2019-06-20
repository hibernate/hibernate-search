/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.GeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LatitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

/**
 * A builder of type or property bridges that map types or property values
 * to a {@link GeoPoint} field, representing a point on earth.
 * <p>
 * These fields allow spatial predicates such as "within" (is the point within a circle, a bounding box, ...),
 * sorts by distance to another point, ...
 *
 * @param <B> The type of the bridge.
 * @see org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge
 * @see #forType()
 * @see #forProperty()
 * @see #latitude()
 * @see #longitude()
 */
public interface GeoPointBridgeBuilder<B> extends BridgeBuilder<B> {

	/**
	 * @param fieldName The name of the {@link GeoPoint} field.
	 * If used on a property, this defaults to the name of that property.
	 * Otherwise, the name must be defined explicitly.
	 * @return {@code this}, for method chaining.
	 */
	GeoPointBridgeBuilder<B> fieldName(String fieldName);

	/**
	 * @param projectable Whether projections are enabled for the {@link GeoPoint} field.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#projectable()
	 * @see Projectable
	 */
	GeoPointBridgeBuilder<B> projectable(Projectable projectable);

	/**
	 * @param sortable Whether the {@link GeoPoint} field should be sortable by distance.
	 * @return {@code this}, for method chaining.
	 * @see GenericField#sortable()
	 * @see Sortable
	 */
	GeoPointBridgeBuilder<B> sortable(Sortable sortable);

	/**
	 * @param markerSet The name of the "marker set".
	 * This is used to discriminate between multiple pairs of latitude/longitude markers:
	 * {@link LatitudeLongitudeMarkerBuilder#markerSet(String) assign a marker set when building each marker},
	 * then select the marker set here.
	 * @return {@code this}, for method chaining.
	 */
	GeoPointBridgeBuilder<B> markerSet(String markerSet);

	/**
	 * @return A {@link GeoPointBridgeBuilder} for use on a type.
	 */
	static GeoPointBridgeBuilder<? extends TypeBridge> forType() {
		return new GeoPointBridge.Builder();
	}

	/**
	 * @return A {@link GeoPointBridgeBuilder} for use on a property.
	 */
	static GeoPointBridgeBuilder<? extends PropertyBridge> forProperty() {
		return new GeoPointBridge.Builder();
	}

	/**
	 * @return A marker builder for the latitude, to be applied on a property.
	 * @see LatitudeLongitudeMarkerBuilder
	 */
	static LatitudeLongitudeMarkerBuilder latitude() {
		return new LatitudeMarker.Builder();
	}

	/**
	 * @return A marker builder for the longitude, to be applied on a property.
	 * @see LatitudeLongitudeMarkerBuilder
	 */
	static LatitudeLongitudeMarkerBuilder longitude() {
		return new LongitudeMarker.Builder();
	}

}
