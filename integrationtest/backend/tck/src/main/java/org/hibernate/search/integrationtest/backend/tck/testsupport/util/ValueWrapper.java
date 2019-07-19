/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Objects;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;

/**
 * A value wrapper used when testing
 * {@link IndexFieldTypeConverterStep#dslConverter(ToDocumentFieldValueConverter) DSL converters}
 * and {@link IndexFieldTypeConverterStep#projectionConverter(FromDocumentFieldValueConverter) projection converters}.
 */
public final class ValueWrapper<T> {
	public static <T> ToDocumentFieldValueConverter<ValueWrapper<T>, T> toIndexFieldConverter() {
		return new ToDocumentFieldValueConverter<ValueWrapper<T>, T>() {
			@Override
			public boolean isValidInputType(Class<?> inputTypeCandidate) {
				return ValueWrapper.class.isAssignableFrom( inputTypeCandidate );
			}

			@Override
			public T convert(ValueWrapper<T> value, ToDocumentFieldValueConvertContext context) {
				return value.getValue();
			}

			@SuppressWarnings("unchecked")
			@Override
			public T convertUnknown(Object value, ToDocumentFieldValueConvertContext context) {
				return ( (ValueWrapper<T>) value ).getValue();
			}

			@Override
			public boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
				return other != null && getClass().equals( other.getClass() );
			}
		};
	}

	public static <T> FromDocumentFieldValueConverter<T, ValueWrapper<T>> fromIndexFieldConverter() {
		return new FromDocumentFieldValueConverter<T, ValueWrapper<T>>() {
			@Override
			public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
				return superTypeCandidate.isAssignableFrom( ValueWrapper.class );
			}

			@Override
			public ValueWrapper<T> convert(T indexedValue,
					FromDocumentFieldValueConvertContext context) {
				return new ValueWrapper<>( indexedValue );
			}

			@Override
			public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
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
