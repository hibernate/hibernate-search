/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.hibernate.search.engine.search.sort.SearchSort;

/**
 * The initial and final step in a composite sort definition, where sort elements can be added.
 * <p>
 * This is only used in "explicit" composite sorts,
 * for example when calling {@link SearchSortFactory#composite()},
 * but not in "implicit" composite sorts such as when calling {@link SortThenStep#then()}.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface CompositeSortComponentsStep<E, S extends CompositeSortComponentsStep<E, ?>>
		extends SortFinalStep, SortThenStep<E> {

	/**
	 * Add an element to the composite sort based on a previously-built {@link SearchSort}.
	 *
	 * @param searchSort The sort to add.
	 * @return {@code this}, for method chaining.
	 */
	S add(SearchSort searchSort);

	/**
	 * Add an element to the composite sort based on an almost-built {@link SearchSort}.
	 *
	 * @param dslFinalStep A final step in the sort DSL allowing the retrieval of a {@link SearchSort}.
	 * @return {@code this}, for method chaining.
	 */
	default S add(SortFinalStep dslFinalStep) {
		return add( dslFinalStep.toSort() );
	}

}
