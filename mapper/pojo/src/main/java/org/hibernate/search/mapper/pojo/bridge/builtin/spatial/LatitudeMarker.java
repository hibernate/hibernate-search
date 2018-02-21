/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial;

import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;

public class LatitudeMarker {

	private final String markerSet;

	/*
	 * Private constructor, use the Builder instead.
	 */
	private LatitudeMarker(String markerSet) {
		this.markerSet = markerSet;
	}

	public String getMarkerSet() {
		return markerSet;
	}

	public static class Builder implements
			AnnotationMarkerBuilder<Latitude> {

		private String markerSet;

		@Override
		public void initialize(Latitude annotation) {
			markerSet( annotation.markerSet() );
		}

		public Builder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public Object build() {
			return new LatitudeMarker( markerSet );
		}

	}
}
