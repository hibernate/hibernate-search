/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultEnumValueBridge<V extends Enum<V>> implements ValueBridge<V, String> {

	private Class<V> enumType;

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + enumType.getName() + "]";
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexSchemaFieldTypedContext<?, String> bind(ValueBridgeBindingContext<V> context) {
		this.enumType = (Class<V>) context.getBridgedElement().getRawType();
		return context.getIndexSchemaFieldContext().asString()
				.projectionConverter( new DefaultEnumFromIndexFieldValueConverter<>( enumType ) );
	}

	@Override
	public String toIndexedValue(V value,
			ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.name();
	}

	@Override
	public V cast(Object value) {
		return enumType.cast( value );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		DefaultEnumValueBridge<?> castedOther = (DefaultEnumValueBridge<?>) other;
		return enumType.equals( castedOther.enumType );
	}

	private static class DefaultEnumFromIndexFieldValueConverter<V extends Enum<V>>
			implements FromIndexFieldValueConverter<String, V> {

		private final Class<V> enumType;

		private DefaultEnumFromIndexFieldValueConverter(Class<V> enumType) {
			this.enumType = enumType;
		}

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( enumType );
		}

		@Override
		public V convert(String indexedValue, FromIndexFieldValueConvertContext context) {
			return indexedValue == null ? null : Enum.valueOf( enumType, indexedValue );
		}

		@Override
		public boolean isCompatibleWith(FromIndexFieldValueConverter<?, ?> other) {
			if ( !getClass().equals( other.getClass() ) ) {
				return false;
			}
			DefaultEnumFromIndexFieldValueConverter<?> castedOther = (DefaultEnumFromIndexFieldValueConverter<?>) other;
			return enumType.equals( castedOther.enumType );
		}
	}

}