/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.ZoneOffset;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.StringHelper;

/**
 * Converts a {@link ZoneOffset} to a {@link String}.
 *
 * @see ZoneOffset#getId()
 * @see ZoneOffset#of(String)
 * @author Davide D'Alto
 */
public class ZoneOffsetBridge implements TwoWayStringBridge {

	public static final ZoneOffsetBridge INSTANCE = new ZoneOffsetBridge();

	private ZoneOffsetBridge() {
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}

		ZoneOffset offSet = (ZoneOffset) object;
		return offSet.getId();
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}

		return ZoneOffset.of( stringValue );
	}

}
