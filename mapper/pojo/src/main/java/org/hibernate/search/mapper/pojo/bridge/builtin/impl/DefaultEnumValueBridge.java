/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.cfg.spi.ValidateUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultEnumValueBridge<V extends Enum<V>> implements ValueBridge<V, String> {

	private Class<V> enumType;

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + enumType.getName() + "]";
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeOptionsStep<?, String> bind(ValueBridgeBindingContext<V> context) {
		this.enumType = (Class<V>) context.getBridgedElement().getRawType();
		return context.getTypeFactory().asString();
	}

	@Override
	public String toIndexedValue(V value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.name();
	}

	@Override
	public V fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : Enum.valueOf( enumType, value );
	}

	@Override
	public V cast(Object value) {
		return enumType.cast( value );
	}

	@Override
	public String parse(String value) {
		ValidateUtils.validateEnum( value, enumType );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		DefaultEnumValueBridge<?> castedOther = (DefaultEnumValueBridge<?>) other;
		return enumType.equals( castedOther.enumType );
	}

}