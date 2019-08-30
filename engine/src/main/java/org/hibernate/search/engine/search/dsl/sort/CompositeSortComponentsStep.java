/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import org.hibernate.search.engine.search.SearchSort;

/**
 * The initial and final step in a composite sort definition, where sort elements can be added.
 * <p>
 * This is only used in "explicit" composite sorts,
 * for example when calling {@link SearchSortFactory#composite()},
 * but not in "implicit" composite sorts such as when calling {@link SortThenStep#then()}.
 */
public interface CompositeSortComponentsStep extends SortFinalStep, SortThenStep {

	/**
	 * Add an element to the composite sort based on a previously-built {@link SearchSort}.
	 *
	 * @param searchSort The sort to add.
	 * @return {@code this}, for method chaining.
	 */
	CompositeSortComponentsStep add(SearchSort searchSort);

	/**
	 * Add an element to the composite sort based on an almost-built {@link SearchSort}.
	 *
	 * @param dslFinalStep A final step in the sort DSL allowing the retrieval of a {@link SearchSort}.
	 * @return {@code this}, for method chaining.
	 */
	default CompositeSortComponentsStep add(SortFinalStep dslFinalStep) {
		return add( dslFinalStep.toSort() );
	}

}
