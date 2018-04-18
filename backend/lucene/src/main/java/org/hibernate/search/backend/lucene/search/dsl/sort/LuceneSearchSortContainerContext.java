/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.sort;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

/**
 * A DSL context allowing to specify the sort order, with some Lucene-specific methods.
 *
 * @param <N> The type of the next context (returned by terminal calls such as {@link ScoreSortContext#end()}
 * or {@link FieldSortContext#end()}).
 */
public interface LuceneSearchSortContainerContext<N> extends SearchSortContainerContext<N> {

	NonEmptySortContext<N> fromLuceneSortField(SortField luceneSortField);

	NonEmptySortContext<N> fromLuceneSort(Sort luceneSort);

}
