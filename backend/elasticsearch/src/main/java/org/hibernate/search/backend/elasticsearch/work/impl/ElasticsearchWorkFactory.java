/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.impl.SearchResultExtractor;
import org.hibernate.search.engine.search.SearchResult;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchWorkFactory {

	ElasticsearchWork<?> createIndex(String indexName, JsonObject model);

	ElasticsearchWork<?> add(String indexName, String id, JsonObject document);

	ElasticsearchWork<?> update(String indexName, String id, JsonObject document);

	ElasticsearchWork<?> delete(String indexName, String id);

	ElasticsearchWork<?> flush(String indexName);

	ElasticsearchWork<?> optimize(String indexName);

	<T> ElasticsearchWork<SearchResult<T>> search(
			Set<String> indexNames, JsonObject payload, SearchResultExtractor<T> searchResultExtractor,
			Long offset, Long limit);

}
