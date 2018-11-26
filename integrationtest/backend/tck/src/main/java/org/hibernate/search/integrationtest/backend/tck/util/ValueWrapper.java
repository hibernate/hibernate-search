/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.util;

import java.util.Objects;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;

/**
 * A value wrapper used when testing
 * {@link IndexSchemaFieldTypedContext#dslConverter(ToIndexFieldValueConverter) DSL converters}
 * and {@link IndexSchemaFieldTypedContext#projectionConverter(FromIndexFieldValueConverter) projection converters}.
 */
public final class ValueWrapper<T> {
	public static <T> ToIndexFieldValueConverter<ValueWrapper<T>, T> toIndexFieldConverter() {
		return new ToIndexFieldValueConverter<ValueWrapper<T>, T>() {
			@Override
			public T convert(ValueWrapper<T> value, ToIndexFieldValueConvertContext context) {
				return value.getValue();
			}

			@SuppressWarnings("unchecked")
			@Override
			public T convertUnknown(Object value, ToIndexFieldValueConvertContext context) {
				return ( (ValueWrapper<T>) value ).getValue();
			}

			@Override
			public boolean isCompatibleWith(ToIndexFieldValueConverter<?, ?> other) {
				return other != null && getClass().equals( other.getClass() );
			}
		};
	}

	public static <T> FromIndexFieldValueConverter<T, ValueWrapper<T>> fromIndexFieldConverter() {
		return new FromIndexFieldValueConverter<T, ValueWrapper<T>>() {

			@Override
			public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
				return superTypeCandidate.isAssignableFrom( ValueWrapper.class );
			}

			@Override
			public ValueWrapper<T> convert(T indexedValue,
					FromIndexFieldValueConvertContext context) {
				return new ValueWrapper<>( indexedValue );
			}

			@Override
			public boolean isCompatibleWith(FromIndexFieldValueConverter<?, ?> other) {
				return other != null && getClass().equals( other.getClass() );
			}
		};
	}

	private final T value;

	public ValueWrapper(T value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + value + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !getClass().equals( obj.getClass() ) ) {
			return false;
		}
		ValueWrapper<?> other = (ValueWrapper<?>) obj;
		return Objects.equals( value, other.value );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( value );
	}

	public T getValue() {
		return value;
	}
}
