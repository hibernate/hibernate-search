/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Objects;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeConverterStep;
import org.hibernate.search.util.impl.integrationtest.common.Normalizable;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

/**
 * A value wrapper used when testing
 * {@link IndexFieldTypeConverterStep#dslConverter(Class, ToDocumentValueConverter)  DSL converters}
 * and {@link IndexFieldTypeConverterStep#projectionConverter(Class, FromDocumentValueConverter)  projection converters}.
 */
public final class ValueWrapper<T> implements Normalizable<ValueWrapper<T>> {
	@SuppressWarnings("rawtypes")
	public static <T> ToDocumentValueConverter<ValueWrapper, T> toDocumentValueConverter() {
		return new ToDocumentValueConverter<ValueWrapper, T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T toDocumentValue(ValueWrapper value, ToDocumentValueConvertContext context) {
				return (T) value.getValue();
			}

			@Override
			public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
				return getClass().equals( other.getClass() );
			}
		};
	}

	@SuppressWarnings("rawtypes")
	public static <T> FromDocumentValueConverter<T, ValueWrapper> fromDocumentValueConverter() {
		return new FromDocumentValueConverter<T, ValueWrapper>() {
			@Override
			public ValueWrapper<T> fromDocumentValue(T indexedValue, FromDocumentValueConvertContext context) {
				return new ValueWrapper<>( indexedValue );
			}

			@Override
			public boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
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
		return Objects.deepEquals( value, other.value );
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
