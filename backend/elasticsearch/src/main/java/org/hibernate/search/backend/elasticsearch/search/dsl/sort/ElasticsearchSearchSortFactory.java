/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort;

import org.hibernate.search.engine.search.dsl.sort.SortThenStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;

/**
 * A factory for search sorts with some Elasticsearch-specific methods.
 */
public interface ElasticsearchSearchSortFactory extends SearchSortFactory {

	/**
	 * Order elements according to a JSON sort definition.
	 *
	 * @param jsonString A string representing an Elasticsearch sort as a JSON object.
	 * The JSON object must be represent a syntactically correct Elasticsearch sort.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-sort.html">the Elasticsearch documentation</a>.
	 * @return A {@link SortThenStep} allowing the retrieval of the sort
	 * or the chaining of other sorts.
	 */
	SortThenStep fromJson(String jsonString);

}
