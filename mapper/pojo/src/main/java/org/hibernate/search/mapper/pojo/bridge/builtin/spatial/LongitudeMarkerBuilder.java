/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial;

import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl.LongitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;

public class LongitudeMarkerBuilder implements AnnotationMarkerBuilder<GeoPointBridge.Longitude> {

	private String markerSet;

	@Override
	public void initialize(GeoPointBridge.Longitude annotation) {
		markerSet( annotation.markerSet() );
	}

	public LongitudeMarkerBuilder markerSet(String markerSet) {
		this.markerSet = markerSet;
		return this;
	}

	@Override
	public Object build() {
		return new LongitudeMarker( markerSet );
	}
}
