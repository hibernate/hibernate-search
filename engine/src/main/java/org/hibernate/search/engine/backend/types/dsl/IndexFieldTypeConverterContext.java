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
 * A context for specifying a field types that support conversion.
 *
 * @param <S> The type of this context.
 * @param <F> The type of field values.
 */
public interface IndexFieldTypeConverterContext<S extends IndexFieldTypeConverterContext<? extends S, F>, F> {

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
