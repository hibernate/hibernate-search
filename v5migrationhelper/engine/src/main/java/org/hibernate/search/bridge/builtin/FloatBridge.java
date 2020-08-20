/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.util.StringHelper;

/**
 * Bridge a {@link Float} to a {@link String}.
 *
 * @see NumericFieldBridge#FLOAT_FIELD_BRIDGE
 * @author Emmanuel Bernard
 */
public class FloatBridge extends NumberBridge {
	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		return Float.valueOf( stringValue );
	}
}
