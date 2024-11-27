/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.dsl;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.ExtendedSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

/**
 * A factory for search sorts with some Lucene-specific methods.
 */
public interface LuceneSearchSortFactory
		extends ExtendedSearchSortFactory<LuceneSearchSortFactory, LuceneSearchPredicateFactory> {

	/**
	 * Order elements by a given Lucene {@link SortField}.
	 *
	 * @param luceneSortField A Lucene sort field.
	 * @return A {@link SortThenStep} allowing the retrieval of the sort
	 * or the chaining of other sorts.
	 */
	SortThenStep fromLuceneSortField(SortField luceneSortField);

	/**
	 * Order elements by a given Lucene {@link Sort}.
	 *
	 * @param luceneSort A Lucene sort.
	 * @return A {@link SortThenStep} allowing the retrieval of the sort
	 * or the chaining of other sorts.
	 */
	SortThenStep fromLuceneSort(Sort luceneSort);

}
