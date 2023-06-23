/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * An abstract base for default bridges that format the property value to a string.
 *
 * @param <V> The type of values on the POJO side of the bridge.
 */
abstract class AbstractStringBasedDefaultBridge<V> extends AbstractSimpleDefaultBridge<V, String> {

	@Override
	public final String toIndexedValue(V value, ValueBridgeToIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		return toString( value );
	}

	@Override
	public final V fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		return fromString( value );
	}

	@Override
	public final String parse(String value) {
		if ( value == null ) {
			return null;
		}
		// Make sure that the value is correctly formatted,
		// and normalize it if necessary.
		return toString( fromString( value ) );
	}

}
