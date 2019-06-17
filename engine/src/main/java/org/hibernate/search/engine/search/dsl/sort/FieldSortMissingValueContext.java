/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.util.common.SearchException;

/**
 * The context used when defining the missing value behavior of a field sort.
 *
 * @param <N> The type of the next context (returned by {@link FieldSortMissingValueContext#sortFirst()}, for example).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortMissingValueContext<N> {

	/**
	 * Put documents with missing values last in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 *
	 * @return The original context, for method chaining.
	 */
	N sortLast();

	/**
	 * Put documents with missing values first in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 *
	 * @return The original context, for method chaining.
	 */
	N sortFirst();

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter#ENABLED}.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @return The original context, for method chaining.
	 * @throws SearchException If the field is not numeric.
	 */
	default N use(Object value) {
		return use( value, DslConverter.ENABLED );
	}

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter}.
	 *
	 * @return The original context, for method chaining.
	 * @throws SearchException If the field is not numeric.
	 */
	N use(Object value, DslConverter dslConverter);

}
