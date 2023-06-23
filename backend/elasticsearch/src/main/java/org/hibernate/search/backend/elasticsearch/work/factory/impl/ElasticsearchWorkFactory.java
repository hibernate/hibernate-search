/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.factory.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkWork;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ClearScrollWork;
import org.hibernate.search.backend.elasticsearch.work.impl.CloseIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.CountWork;
import org.hibernate.search.backend.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteByQueryWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DropIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.ExplainWork;
import org.hibernate.search.backend.elasticsearch.work.impl.FlushWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ForceMergeWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexMetadataWork;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.OpenIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexAliasesWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexMappingWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexSettingsWork;
import org.hibernate.search.backend.elasticsearch.work.impl.RefreshWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ScrollWork;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.WaitForIndexStatusWork;

import com.google.gson.JsonObject;

public interface ElasticsearchWorkFactory {

	IndexWork.Builder index(String entityTypeName, Object entityIdentifier,
			URLEncodedString elasticsearchIndexName,
			String documentIdentifier, String routingKey, JsonObject document);

	DeleteWork.Builder delete(String entityTypeName, Object entityIdentifier,
			URLEncodedString elasticsearchIndexName,
			String documentIdentifier, String routingKey);

	DeleteByQueryWork.Builder deleteByQuery(URLEncodedString indexName, JsonObject payload);

	FlushWork.Builder flush();

	RefreshWork.Builder refresh();

	ForceMergeWork.Builder mergeSegments();

	BulkWork.Builder bulk(List<? extends BulkableWork<?>> bulkableWorks);

	<T> SearchWork.Builder<T> search(JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor);

	CountWork.Builder count();

	ExplainWork.Builder explain(URLEncodedString indexName, URLEncodedString id, JsonObject payload);

	<T> ScrollWork.Builder<T> scroll(String scrollId, String scrollTimeout,
			ElasticsearchSearchResultExtractor<T> searchResultExtractor);

	ClearScrollWork.Builder clearScroll(String scrollId);

	CreateIndexWork.Builder createIndex(URLEncodedString indexName);

	DropIndexWork.Builder dropIndex(URLEncodedString indexName);

	OpenIndexWork.Builder openIndex(URLEncodedString indexName);

	CloseIndexWork.Builder closeIndex(URLEncodedString indexName);

	GetIndexMetadataWork.Builder getIndexMetadata();

	PutIndexSettingsWork.Builder putIndexSettings(URLEncodedString indexName, IndexSettings settings);

	PutIndexMappingWork.Builder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping);

	WaitForIndexStatusWork.Builder waitForIndexStatusWork(URLEncodedString indexName, IndexStatus requiredStatus,
			int requiredStatusTimeoutInMs);

	PutIndexAliasesWork.Builder putIndexAliases(URLEncodedString indexName, Map<String, IndexAliasDefinition> aliases);

}
