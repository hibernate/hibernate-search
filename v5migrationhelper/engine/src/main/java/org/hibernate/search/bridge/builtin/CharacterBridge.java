/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge a {@link Character} to a {@link String}.
 *
 * @author Davide D'Alto
 */
public class CharacterBridge implements TwoWayStringBridge {

	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		if ( stringValue.length() > 1 ) {
			throw new IllegalArgumentException( "<" + stringValue + "> is not a char" );
		}
		return stringValue.charAt( 0 );
	}

	@Override
	public String objectToString(Object object) {
		return object == null
				? null
				: object.toString();
	}
}
