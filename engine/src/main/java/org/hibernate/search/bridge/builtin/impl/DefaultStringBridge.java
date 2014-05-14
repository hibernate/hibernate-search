/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.builtin.impl;

import org.hibernate.search.bridge.StringBridge;

/**
 * Convert an object using the toString method or return {@code null} if the object is {@code null}.
 *
 * @author Davide D'Alto
 */
public class DefaultStringBridge implements StringBridge {

	public static final DefaultStringBridge INSTANCE = new DefaultStringBridge();

	private DefaultStringBridge() {
		//don't create instances
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		return object.toString();
	}

}
