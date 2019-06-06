/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort;

import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortTerminalContext;

/**
 * A DSL context allowing to specify the sort order, with some Elasticsearch-specific methods.
 */
public interface ElasticsearchSearchSortFactoryContext extends SearchSortFactoryContext {

	/**
	 * Order elements according to a JSON sort definition.
	 *
	 * @param jsonString A string representing an Elasticsearch sort as a JSON object.
	 * The JSON object must be represent a syntactically correct Elasticsearch sort.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-sort.html">the Elasticsearch documentation</a>.
	 * @return A context allowing to {@link NonEmptySortContext#then() chain other sorts}
	 * or {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 */
	NonEmptySortContext fromJson(String jsonString);

}
