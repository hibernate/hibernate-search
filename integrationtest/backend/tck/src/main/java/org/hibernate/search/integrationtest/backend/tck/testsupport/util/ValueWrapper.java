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
import org.hibernate.search.util.impl.integrationtest.common.Normalizable;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

/**
 * A value wrapper used when testing
 * {@link IndexFieldTypeConverterStep#dslConverter(Class, ToDocumentFieldValueConverter)  DSL converters}
 * and {@link IndexFieldTypeConverterStep#projectionConverter(Class, FromDocumentFieldValueConverter)  projection converters}.
 */
public final class ValueWrapper<T> implements Normalizable<ValueWrapper<T>> {
	public static <T> ToDocumentFieldValueConverter<ValueWrapper, T> toIndexFieldConverter() {
		return new ToDocumentFieldValueConverter<ValueWrapper, T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T convert(ValueWrapper value, ToDocumentFieldValueConvertContext context) {
				return (T) value.getValue();
			}

			@Override
			public boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
				return getClass().equals( other.getClass() );
			}
		};
	}

	public static <T> FromDocumentFieldValueConverter<T, ValueWrapper> fromIndexFieldConverter() {
		return new FromDocumentFieldValueConverter<T, ValueWrapper>() {
			@Override
			public ValueWrapper<T> convert(T indexedValue, FromDocumentFieldValueConvertContext context) {
				return new ValueWrapper<>( indexedValue );
			}

			@Override
			public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
				return getClass().equals( other.getClass() );
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

	@Override
	public ValueWrapper<T> normalize() {
		return new ValueWrapper<>( NormalizationUtils.normalize( value ) );
	}

	public T getValue() {
		return value;
	}
}
