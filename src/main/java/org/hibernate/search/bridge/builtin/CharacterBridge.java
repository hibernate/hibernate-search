// $Id:$
package org.hibernate.search.bridge.builtin;

import org.hibernate.util.StringHelper;

/**
 * Map a character element
 *
 * @author Davide D'Alto
 */
public class CharacterBridge implements org.hibernate.search.bridge.TwoWayStringBridge {

	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		if ( stringValue.length() > 1 ) {
			throw new IllegalArgumentException( "<" + stringValue + "> is not a char" );
		}
		return stringValue.charAt( 0 );
	}

	public String objectToString(Object object) {
		return object == null
				? null
				: object.toString();
	}
}
