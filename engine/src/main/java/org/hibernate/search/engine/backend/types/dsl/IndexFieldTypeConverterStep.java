/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;

/**
 * The step in an index field type definition where DSL converters (applied when using the search DSL)
 * and projection converters (applied when projecting in a search query) can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
public interface IndexFieldTypeConverterStep<S extends IndexFieldTypeConverterStep<?, F>, F> {

	/**
	 * Define how values passed to the predicate and sort DSL should be converted to the type of field values.
	 * <p>
	 * When not set, users are expected to pass the field's value type directly.
	 *
	 * @param valueType The type of values that can be passed to the DSL.
	 * @param toIndexConverter A converter from the given value type to the index field type.
	 * @param <V> The type of values that can be passed to the DSL.
	 * @return {@code this}, for method chaining.
	 */
	<V> S dslConverter(Class<V> valueType, ToDocumentValueConverter<V, ? extends F> toIndexConverter);

	/**
	 * Define how values returned when projecting on fields of this type
	 * should be converted before being returned to the user.
	 * <p>
	 * When not set, users will be returned the field's value type directly.
	 *
	 * @param valueType The type of values that will be returned when projecting on fields of this type.
	 * @param fromIndexConverter A converter from the index field type to the given value type.
	 * @param <V> The type of values that will be returned when projecting on fields of this type.
	 * @return {@code this}, for method chaining.
	 */
	<V> S projectionConverter(Class<V> valueType, FromDocumentValueConverter<? super F, V> fromIndexConverter);

}
