/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge a {@link String} to a string.
 *
 * @author Emmanuel Bernard
 */
public class StringBridge implements TwoWayStringBridge {

	public static final StringBridge INSTANCE = new StringBridge();

	@Override
	public Object stringToObject(String stringValue) {
		return stringValue;
	}

	@Override
	public String objectToString(Object object) {
		return (String) object;
	}
}
