/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.SearchException;

/**
 * The step in a sort definition where the behavior on missing values can be set.
 *
 * @param <N> The type of the next step (returned by {@link FieldSortMissingValueBehaviorStep#sortFirst()}, for example).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortMissingValueBehaviorStep<N> {

	/**
	 * Put documents with missing values last in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 *
	 * @return The next step.
	 */
	N sortLast();

	/**
	 * Put documents with missing values first in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 *
	 * @return The next step.
	 */
	N sortFirst();

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert#YES}.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @return The next step.
	 * @throws SearchException If the field is not numeric.
	 */
	default N use(Object value) {
		return use( value, ValueConvert.YES );
	}

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueConvert}.
	 * @return The next step.
	 * @throws SearchException If the field is not numeric.
	 */
	N use(Object value, ValueConvert convert);

}
