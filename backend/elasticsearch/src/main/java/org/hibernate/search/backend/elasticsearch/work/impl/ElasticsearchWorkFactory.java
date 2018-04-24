/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchResultExtractor;
import org.hibernate.search.engine.search.SearchResult;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
// TODO restore the full work factory from Search 5
public interface ElasticsearchWorkFactory {

	ElasticsearchWork<?> dropIndexIfExists(URLEncodedString indexName);

	ElasticsearchWork<?> createIndex(URLEncodedString indexName, URLEncodedString typeName, RootTypeMapping mapping);

	ElasticsearchWork<?> add(URLEncodedString indexName, URLEncodedString typeName, String id, String routingKey, JsonObject document);

	ElasticsearchWork<?> update(URLEncodedString indexName, URLEncodedString typeName, String id, String routingKey, JsonObject document);

	ElasticsearchWork<?> delete(URLEncodedString indexName, URLEncodedString typeName, String id, String routingKey);

	ElasticsearchWork<?> flush(URLEncodedString indexName);

	ElasticsearchWork<?> optimize(URLEncodedString indexName);

	<T> ElasticsearchWork<SearchResult<T>> search(Set<URLEncodedString> indexNames, Set<String> routingKeys,
			JsonObject payload, SearchResultExtractor<T> searchResultExtractor,
			Long offset, Long limit);

}
