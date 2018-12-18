/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.GeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LatitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

public interface GeoPointBridgeBuilder<B> extends BridgeBuilder<B> {

	GeoPointBridgeBuilder<B> fieldName(String fieldName);

	GeoPointBridgeBuilder<B> projectable(Projectable projectable);

	GeoPointBridgeBuilder<B> markerSet(String markerSet);

	static GeoPointBridgeBuilder<? extends TypeBridge> forType() {
		return new GeoPointBridge.Builder();
	}

	static GeoPointBridgeBuilder<? extends PropertyBridge> forProperty() {
		return new GeoPointBridge.Builder();
	}

	static LatitudeLongitudeMarkerBuilder latitude() {
		return new LatitudeMarker.Builder();
	}

	static LatitudeLongitudeMarkerBuilder longitude() {
		return new LongitudeMarker.Builder();
	}

}
