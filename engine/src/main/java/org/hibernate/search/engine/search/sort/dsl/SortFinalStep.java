/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.hibernate.search.engine.search.sort.SearchSort;

/**
 * The final step in a sort definition, where the sort can be retrieved.
 */
public interface SortFinalStep {

	/**
	 * Create a {@link SearchSort} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchSort} resulting from the previous DSL steps.
	 */
	SearchSort toSort();

}
