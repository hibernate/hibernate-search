/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Base class used to bridge numbers (integer, double, etc.) as strings.
 * <p>
 * Note that it is possible to store types as numeric fields using the bridges provided by {@link NumericFieldBridge}.
 *
 * @see NumericFieldBridge
 * @author Emmanuel Bernard
 */
public abstract class NumberBridge implements TwoWayStringBridge {
	@Override
	public String objectToString(Object object) {
		return object != null ?
				object.toString() :
				null;
	}
}
