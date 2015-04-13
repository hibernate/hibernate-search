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
 * Bridge a boolean field to a {@link String}.
 *
 * @author Sylvain Vieujot
 */
public class BooleanBridge implements TwoWayStringBridge {

	@Override
	public Boolean stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		return Boolean.valueOf( stringValue );
	}

	@Override
	public String objectToString(Object object) {
		return object == null ?
				null :
				object.toString();
	}
}
