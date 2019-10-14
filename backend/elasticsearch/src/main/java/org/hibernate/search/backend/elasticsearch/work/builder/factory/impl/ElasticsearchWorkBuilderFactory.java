/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.index.settings.esnative.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.BulkWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ClearScrollWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CloseIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CountWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CreateIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DeleteByQueryWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DeleteWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DropIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ExplainWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.FlushWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.GetIndexMetadataWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexExistsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.OpenIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.OptimizeWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexMappingWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexSettingsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.RefreshWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ScrollWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.WaitForIndexStatusWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;

import com.google.gson.JsonObject;


public interface ElasticsearchWorkBuilderFactory {

	IndexWorkBuilder index(String hibernateSearchIndexName, URLEncodedString elasticsearchIndexName,
			URLEncodedString id, String routingKey, JsonObject document);

	DeleteWorkBuilder delete(String hibernateSearchIndexName, URLEncodedString elasticsearchIndexName,
			URLEncodedString id, String routingKey);

	DeleteByQueryWorkBuilder deleteByQuery(URLEncodedString indexName, JsonObject payload);

	FlushWorkBuilder flush();

	RefreshWorkBuilder refresh();

	OptimizeWorkBuilder optimize();

	BulkWorkBuilder bulk(List<? extends BulkableElasticsearchWork<?>> bulkableWorks);

	<T> SearchWorkBuilder<T> search(JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor);

	CountWorkBuilder count(Set<URLEncodedString> indexNames);

	ExplainWorkBuilder explain(URLEncodedString indexName, URLEncodedString id, JsonObject payload);

	<T> ScrollWorkBuilder<T> scroll(String scrollId, String scrollTimeout, ElasticsearchSearchResultExtractor<T> searchResultExtractor);

	ClearScrollWorkBuilder clearScroll(String scrollId);

	CreateIndexWorkBuilder createIndex(URLEncodedString indexName);

	DropIndexWorkBuilder dropIndex(URLEncodedString indexName);

	OpenIndexWorkBuilder openIndex(URLEncodedString indexName);

	CloseIndexWorkBuilder closeIndex(URLEncodedString indexName);

	IndexExistsWorkBuilder indexExists(URLEncodedString indexName);

	GetIndexMetadataWorkBuilder getIndexMetadata(URLEncodedString indexName);

	PutIndexSettingsWorkBuilder putIndexSettings(URLEncodedString indexName, IndexSettings settings);

	PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping);

	WaitForIndexStatusWorkBuilder waitForIndexStatusWork(URLEncodedString indexName, IndexStatus requiredStatus, String timeout);

}
