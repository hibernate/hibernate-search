/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort;

import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

/**
 * A DSL context allowing to specify the type of a predicate, with some Elasticsearch-specific methods.
 *
 * @param <N> The type of the next context (returned by terminal calls such as {@link ScoreSortContext#end()}
 * or {@link FieldSortContext#end()}).
 */
public interface ElasticsearchSearchSortContainerContext<N> extends SearchSortContainerContext<N> {

	NonEmptySortContext<N> fromJsonString(String jsonString);

}
