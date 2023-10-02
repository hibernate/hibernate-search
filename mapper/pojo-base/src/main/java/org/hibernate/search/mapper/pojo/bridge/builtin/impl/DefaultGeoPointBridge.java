/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.engine.spatial.GeoPoint;

public final class DefaultGeoPointBridge extends AbstractPassThroughDefaultBridge<GeoPoint> {

	public static final DefaultGeoPointBridge INSTANCE = new DefaultGeoPointBridge();

	private DefaultGeoPointBridge() {
	}

	@Override
	protected String toString(GeoPoint value) {
		return value.latitude() + ", " + value.longitude();
	}

	@Override
	protected GeoPoint fromString(String value) {
		return ParseUtils.parseGeoPoint( value );
	}

}
