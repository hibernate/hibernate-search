/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

/**
 * The step in an index field type definition where DSL converters (applied when using the search DSL)
 * and projection converters (applied when projecting in a search query) can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
public interface IndexFieldTypeConverterStep<S extends IndexFieldTypeConverterStep<? extends S, F>, F> {

	/**
	 * Define how values passed to the predicate and sort DSL should be converted to the type of field values.
	 * <p>
	 * When not set, users are expected to pass the field's value type directly.
	 *
	 * @param toIndexConverter A converter.
	 * @return {@code this}, for method chaining.
	 */
	S dslConverter(ToDocumentFieldValueConverter<?, ? extends F> toIndexConverter);

	/**
	 * Define how values returned when projecting on fields of this type
	 * should be converted before being returned to the user.
	 * <p>
	 * When not set, users will be returned the field's value type directly.
	 *
	 * @param fromIndexConverter A converter.
	 * @return {@code this}, for method chaining.
	 */
	S projectionConverter(FromDocumentFieldValueConverter<? super F, ?> fromIndexConverter);

}
