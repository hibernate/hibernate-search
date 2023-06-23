/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * An abstract base for default bridges that convert the property value to an equivalent value in another type,
 * which is supported directly by the backend,
 * and implement string-related operations
 * ({@link IdentifierBridge#toDocumentIdentifier(Object, IdentifierBridgeToDocumentIdentifierContext)},
 * {@link ValueBridge#parse(String)}, ...) directly.
 *
 * @param <V> The type of values on the POJO side of the bridge.
 * @param <F> The type of converted values, which is the type of raw index field values, on the index side of the bridge.
 */
abstract class AbstractConvertingDefaultBridge<V, F> extends AbstractSimpleDefaultBridge<V, F> {

	@Override
	public final F toIndexedValue(V value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : toConvertedValue( value );
	}

	@Override
	public final V fromIndexedValue(F value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : fromConvertedValue( value );
	}

	@Override
	public final F parse(String value) {
		return value == null ? null : toConvertedValue( fromString( value ) );
	}

	protected abstract F toConvertedValue(V value);

	protected abstract V fromConvertedValue(F value);

}
