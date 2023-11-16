/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.metamodel;

import java.util.Optional;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The type of a "value" field in the index,
 * exposing its various capabilities and accepted Java types.
 *
 * @see IndexValueFieldDescriptor
 */
public interface IndexValueFieldTypeDescriptor {

	/**
	 * @return {@code true} if search predicates are supported on fields of this type.
	 * Some sorts may still be unsupported because they don't make sense
	 * (e.g. a "within circle" predicate on a string field).
	 */
	boolean searchable();


	/**
	 * @return {@code true} if sorts are supported on fields of this type.
	 * Some sorts may still be unsupported because they don't make sense
	 * (e.g. a distance sort aggregation on a string field).
	 */
	boolean sortable();

	/**
	 * @return {@code true} if projections are supported on fields of this type.
	 * Some projections may still be unsupported because they don't make sense
	 * (e.g. a distance projection aggregation on a string field).
	 */
	boolean projectable();

	/**
	 * @return {@code true} if aggregations are supported on fields of this type.
	 * Some aggregations may still be unsupported because they don't make sense
	 * (e.g. a range aggregation on an analyzed string field).
	 */
	boolean aggregable();

	/**
	 * @return {@code true} if the field type allows storing multiple values, {@code false} otherwise.
	 */
	@Incubating
	boolean multivaluable();

	/**
	 * @return The expected raw Java type of arguments passed to the DSL for this field.
	 * @see org.hibernate.search.engine.search.common.ValueConvert#YES
	 * @see org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter
	 */
	Class<?> dslArgumentClass();

	/**
	 * @return The raw Java type of projected values for this field.
	 * @see org.hibernate.search.engine.search.common.ValueConvert#YES
	 * @see org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter
	 */
	Class<?> projectedValueClass();

	/**
	 * @return The raw Java type of values for this field.
	 * This may not be the expected type for arguments passed to the DSL for this field,
	 * nor the type of projected values for this field.
	 * See {@link #dslArgumentClass()}
	 */
	Class<?> valueClass();

	/**
	 * @return The name of the analyzer assigned to this type, if any.
	 * Only ever set for String fields.
	 */
	Optional<String> analyzerName();

	/**
	 * @return The name of the normalizer assigned to this type, if any.
	 * Only ever set for String fields.
	 */
	Optional<String> normalizerName();

	/**
	 * @return The name of the search analyzer assigned to this type, if any.
	 * Only ever set for String fields.
	 * By default, the search analyzer is the same as the {@link #analyzerName() analyzer}.
	 */
	Optional<String> searchAnalyzerName();

}
