/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;

final class ValueBridgeToIndexFieldValueConverter<U, V extends U, F> implements ToIndexFieldValueConverter<V, F> {
	private final ValueBridge<U, F> bridge;

	ValueBridgeToIndexFieldValueConverter(ValueBridge<U, F> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public F convert(V value) {
		return bridge.toIndexedValue( value );
	}

	@Override
	public F convertUnknown(Object value) {
		return bridge.toIndexedValue( bridge.cast( value ) );
	}

	@Override
	public boolean isCompatibleWith(ToIndexFieldValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ValueBridgeToIndexFieldValueConverter<?, ?, ?> castedOther =
				(ValueBridgeToIndexFieldValueConverter<?, ?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}
}
