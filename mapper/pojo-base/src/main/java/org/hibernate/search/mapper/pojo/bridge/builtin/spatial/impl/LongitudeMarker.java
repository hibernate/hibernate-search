/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.LatitudeLongitudeMarkerBinder;

public class LongitudeMarker {

	private final String markerSet;

	/**
	 * Private constructor, use {@link GeoPointBinder#longitude()} instead.
	 */
	private LongitudeMarker(String markerSet) {
		this.markerSet = markerSet;
	}

	public String getMarkerSet() {
		return markerSet;
	}

	public static class Binder implements LatitudeLongitudeMarkerBinder {

		private String markerSet;

		@Override
		public Binder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public void bind(MarkerBindingContext context) {
			context.marker( new LongitudeMarker( markerSet ) );
		}
	}

}
