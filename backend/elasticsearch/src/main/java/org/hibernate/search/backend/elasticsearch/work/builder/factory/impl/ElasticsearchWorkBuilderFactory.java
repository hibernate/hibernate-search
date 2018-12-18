/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ClearScrollWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CloseIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CreateIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DeleteByQueryWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DeleteWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DropIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ExplainWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.FlushWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.GetIndexSettingsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexExistsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.OpenIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.OptimizeWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexSettingsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.RefreshWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ScrollWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchWorkBuilderFactory {

	IndexWorkBuilder index(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String routingKey, JsonObject document);

	DeleteWorkBuilder delete(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String routingKey);

	DeleteByQueryWorkBuilder deleteByQuery(URLEncodedString indexName, JsonObject payload);

	FlushWorkBuilder flush();

	RefreshWorkBuilder refresh();

	OptimizeWorkBuilder optimize();

	// TODO restore method => BulkWorkBuilder bulk(List<BulkableElasticsearchWork<?>> bulkableWorks);

	<T> SearchWorkBuilder<T> search(JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor);

	ExplainWorkBuilder explain(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject payload);

	<T> ScrollWorkBuilder<T> scroll(String scrollId, String scrollTimeout, ElasticsearchSearchResultExtractor<T> searchResultExtractor);

	ClearScrollWorkBuilder clearScroll(String scrollId);

	CreateIndexWorkBuilder createIndex(URLEncodedString indexName);

	DropIndexWorkBuilder dropIndex(URLEncodedString indexName);

	OpenIndexWorkBuilder openIndex(URLEncodedString indexName);

	CloseIndexWorkBuilder closeIndex(URLEncodedString indexName);

	IndexExistsWorkBuilder indexExists(URLEncodedString indexName);

	GetIndexSettingsWorkBuilder getIndexSettings(URLEncodedString indexName);

	PutIndexSettingsWorkBuilder putIndexSettings(URLEncodedString indexName, IndexSettings settings);

	// TODO restore method => GetIndexTypeMappingsWorkBuilder getIndexTypeMappings(URLEncodedString indexName);

	// TODO restore method => PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, URLEncodedString typeName, TypeMapping mapping);

	// TODO restore method => WaitForIndexStatusWorkBuilder waitForIndexStatusWork(URLEncodedString indexName, ElasticsearchIndexStatus requiredStatus, String timeout);

}
