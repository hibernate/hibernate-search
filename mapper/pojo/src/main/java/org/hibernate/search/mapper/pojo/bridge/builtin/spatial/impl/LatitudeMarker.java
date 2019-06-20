/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl;

import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.GeoPointBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.LatitudeLongitudeMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuildContext;

public class LatitudeMarker {

	public static class Builder implements LatitudeLongitudeMarkerBuilder, AnnotationMarkerBuilder<Latitude> {

		private String markerSet;

		@Override
		public void initialize(Latitude annotation) {
			markerSet( annotation.markerSet() );
		}

		@Override
		public Builder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public Object build(MarkerBuildContext buildContext) {
			return new LatitudeMarker( markerSet );
		}

	}

	private final String markerSet;

	/**
	 * Private constructor, use {@link GeoPointBridgeBuilder#latitude()} instead.
	 */
	private LatitudeMarker(String markerSet) {
		this.markerSet = markerSet;
	}

	public String getMarkerSet() {
		return markerSet;
	}
}
