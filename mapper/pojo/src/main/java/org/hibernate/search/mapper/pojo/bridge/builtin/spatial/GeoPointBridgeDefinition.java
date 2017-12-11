/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial;

import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeDefinitionBase;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerDefinitionBase;


/**
 * @author Yoann Rodiere
 */
public class GeoPointBridgeDefinition extends BridgeDefinitionBase<GeoPointBridge> {

	@Override
	protected Class<GeoPointBridge> getAnnotationClass() {
		return GeoPointBridge.class;
	}

	public GeoPointBridgeDefinition fieldName(String name) {
		addParameter( "fieldName", name );
		return this;
	}

	public GeoPointBridgeDefinition store(Store store) {
		addParameter( "store", store );
		return this;
	}

	public GeoPointBridgeDefinition markerSet(String markerSet) {
		addParameter( "markerSet", markerSet );
		return this;
	}

	public static class LatitudeMarkerDefinition extends MarkerDefinitionBase<GeoPointBridge.Latitude> {
		@Override
		protected Class<GeoPointBridge.Latitude> getAnnotationClass() {
			return GeoPointBridge.Latitude.class;
		}

		public LatitudeMarkerDefinition markerSet(String markerSet) {
			addParameter( "markerSet", markerSet );
			return this;
		}
	}

	public static class LongitudeMarkerDefinition extends MarkerDefinitionBase<GeoPointBridge.Longitude> {
		@Override
		protected Class<GeoPointBridge.Longitude> getAnnotationClass() {
			return GeoPointBridge.Longitude.class;
		}

		public LongitudeMarkerDefinition markerSet(String markerSet) {
			addParameter( "markerSet", markerSet );
			return this;
		}
	}
}
