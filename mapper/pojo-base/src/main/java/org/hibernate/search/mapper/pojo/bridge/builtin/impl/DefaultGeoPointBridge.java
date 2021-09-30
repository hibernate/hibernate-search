/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
