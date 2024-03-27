/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.search.sort.SearchSort;

/**
 * A factory for search sort builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search sorts.
 */
public interface SearchSortBuilderFactory {

	ScoreSortBuilder score();

	SearchSort indexOrder();

	CompositeSortBuilder composite();

}
