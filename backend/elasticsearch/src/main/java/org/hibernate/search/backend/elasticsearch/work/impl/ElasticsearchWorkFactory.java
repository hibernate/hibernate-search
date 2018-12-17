/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchLoadableSearchResult;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
// TODO restore the full work factory from Search 5
public interface ElasticsearchWorkFactory {

	ElasticsearchWork<?> dropIndexIfExists(URLEncodedString indexName);

	ElasticsearchWork<?> createIndex(URLEncodedString indexName, URLEncodedString typeName, RootTypeMapping mapping,
			IndexSettings settings);

	ElasticsearchWork<?> update(URLEncodedString indexName, URLEncodedString typeName, String id, String routingKey, JsonObject document);

	ElasticsearchWork<?> delete(URLEncodedString indexName, URLEncodedString typeName, String id, String routingKey);

	ElasticsearchWork<?> deleteAll(URLEncodedString indexName, String tenantId);

	ElasticsearchWork<?> flush(URLEncodedString indexName);

	ElasticsearchWork<?> optimize(URLEncodedString indexName);

	<T> ElasticsearchWork<ElasticsearchLoadableSearchResult<T>> search(Set<URLEncodedString> indexNames, Set<String> routingKeys,
			JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor,
			Long offset, Long limit);

	ElasticsearchWork<Long> count(Set<URLEncodedString> indexNames, Set<String> routingKeys, JsonObject payload);

}
