/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.dsl;

import org.hibernate.search.bridge.StringBridge;

/**
 * Example of a StringBridge expecting to be applied on numeric objects.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class RomanNumberFieldBridge implements StringBridge {

	@Override
	public String objectToString(Object object) {
		if ( !( object instanceof Number ) ) {
			return null;
		}
		int v = ( (Number) object ).intValue();
		if ( v == 0 ) {
			return null;
		}
		if ( v == 1 ) {
			return "I";
		}
		if ( v == 2 ) {
			return "II";
		}
		if ( v == 3 ) {
			return "III";
		}
		if ( v == 4 ) {
			return "IV";
		}
		if ( v == 5 ) {
			return "IV";
		}
		// ... I bet someone has written a smarter converter
		return null;
	}

}
