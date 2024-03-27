/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.LatitudeLongitudeMarkerBinder;

public class LatitudeMarker {

	private final String markerSet;

	/**
	 * Private constructor, use {@link GeoPointBinder#latitude()} instead.
	 */
	private LatitudeMarker(String markerSet) {
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
			context.marker( new LatitudeMarker( markerSet ) );
		}

	}
}
