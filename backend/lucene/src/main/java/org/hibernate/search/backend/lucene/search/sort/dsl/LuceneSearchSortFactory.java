/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
