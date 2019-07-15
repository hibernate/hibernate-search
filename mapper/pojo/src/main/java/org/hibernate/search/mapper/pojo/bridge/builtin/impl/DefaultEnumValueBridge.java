/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ValidateUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultEnumValueBridge<V extends Enum<V>> implements ValueBridge<V, String> {

	private final Class<V> enumType;

	private DefaultEnumValueBridge(Class<V> enumType) {
		this.enumType = enumType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + enumType.getName() + "]";
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

	public static class Binder implements ValueBinder {
		@Override
		@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
		public void bind(ValueBindingContext<?> context) {
			doBind( context, (Class) context.getBridgedElement().getRawType() );
		}

		private <V extends Enum<V>> void doBind(ValueBindingContext<?> context, Class<V> enumType) {
			context.setBridge( enumType, new DefaultEnumValueBridge<>( enumType ) );
		}
	}

}