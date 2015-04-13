/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import java.util.UUID;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge a {@link UUID} to a {@link String}.
 *
 * @author Davide D'Alto
 */
public class UUIDBridge implements TwoWayStringBridge {

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		return object.toString();
	}

	@Override
	public UUID stringToObject(String stringValue) {
		if ( stringValue == null || stringValue.isEmpty() ) {
			return null;
		}
		return UUID.fromString( stringValue );
	}
}
