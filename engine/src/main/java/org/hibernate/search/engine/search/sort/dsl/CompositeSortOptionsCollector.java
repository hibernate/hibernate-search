/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.hibernate.search.engine.search.sort.SearchSort;

public interface CompositeSortOptionsCollector<S extends CompositeSortOptionsCollector<?>> {

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
