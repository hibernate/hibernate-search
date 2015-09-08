/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.ZoneId;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.StringHelper;

/**
 * Converts a {@link ZoneId} to a {@link String}.
 *
 * @see ZoneId#getId()
 * @see ZoneId#of(String)
 * @author Davide D'Alto
 */
public class ZoneIdBridge implements TwoWayStringBridge {

	public static final ZoneIdBridge INSTANCE = new ZoneIdBridge();

	private ZoneIdBridge() {
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}

		ZoneId zoneId = (ZoneId) object;
		return zoneId.getId();
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}

		return ZoneId.of( stringValue );
	}
}
