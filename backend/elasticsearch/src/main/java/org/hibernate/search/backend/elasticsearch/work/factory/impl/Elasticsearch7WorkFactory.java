/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.factory.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
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

/**
 * A work builder factory for ES7.0+.
 */
public class Elasticsearch7WorkFactory implements ElasticsearchWorkFactory {

	protected final GsonProvider gsonProvider;
	private final Boolean ignoreShardFailures;

	public Elasticsearch7WorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures) {
		this.gsonProvider = gsonProvider;
		this.ignoreShardFailures = ignoreShardFailures;
	}

	@Override
	public IndexWork.Builder index(String entityTypeName, Object entityIdentifier,
			URLEncodedString elasticsearchIndexName,
			String documentIdentifier, String routingKey, JsonObject document) {
		return IndexWork.Builder.create( entityTypeName, entityIdentifier,
				elasticsearchIndexName, documentIdentifier, routingKey, document );
	}

	@Override
	public DeleteWork.Builder delete(String entityTypeName, Object entityIdentifier,
			URLEncodedString elasticsearchIndexName, String documentIdentifier, String routingKey) {
		return DeleteWork.Builder.create( entityTypeName, entityIdentifier,
				elasticsearchIndexName, documentIdentifier, routingKey );
	}

	@Override
	public DeleteByQueryWork.Builder deleteByQuery(URLEncodedString indexName, JsonObject payload) {
		return new DeleteByQueryWork.Builder( indexName, payload, this );
	}

	@Override
	public FlushWork.Builder flush() {
		return new FlushWork.Builder();
	}

	@Override
	public RefreshWork.Builder refresh() {
		return new RefreshWork.Builder();
	}

	@Override
	public ForceMergeWork.Builder mergeSegments() {
		return new ForceMergeWork.Builder();
	}

	@Override
	public BulkWork.Builder bulk(List<? extends BulkableWork<?>> bulkableWorks) {
		return new BulkWork.Builder( bulkableWorks );
	}

	@Override
	public <T> SearchWork.Builder<T> search(JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		SearchWork.Builder<T> builder = SearchWork.Builder.create( payload, searchResultExtractor );
		if ( ignoreShardFailures ) {
			builder.ignoreShardFailures();
		}
		return builder;
	}

	@Override
	public CountWork.Builder count() {
		return new CountWork.Builder();
	}

	@Override
	public ExplainWork.Builder explain(URLEncodedString indexName, URLEncodedString id, JsonObject payload) {
		return ExplainWork.Builder.create( indexName, id, payload );
	}

	@Override
	public <T> ScrollWork.Builder<T> scroll(String scrollId, String scrollTimeout,
			ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return new ScrollWork.Builder<>( scrollId, scrollTimeout, searchResultExtractor );
	}

	@Override
	public ClearScrollWork.Builder clearScroll(String scrollId) {
		return new ClearScrollWork.Builder( scrollId );
	}

	@Override
	public CreateIndexWork.Builder createIndex(URLEncodedString indexName) {
		return CreateIndexWork.Builder.create( gsonProvider, indexName );
	}

	@Override
	public DropIndexWork.Builder dropIndex(URLEncodedString indexName) {
		return new DropIndexWork.Builder( indexName );
	}

	@Override
	public OpenIndexWork.Builder openIndex(URLEncodedString indexName) {
		return new OpenIndexWork.Builder( indexName );
	}

	@Override
	public CloseIndexWork.Builder closeIndex(URLEncodedString indexName) {
		return new CloseIndexWork.Builder( indexName );
	}

	@Override
	public GetIndexMetadataWork.Builder getIndexMetadata() {
		return GetIndexMetadataWork.Builder.create();
	}

	@Override
	public PutIndexSettingsWork.Builder putIndexSettings(URLEncodedString indexName, IndexSettings settings) {
		return new PutIndexSettingsWork.Builder( gsonProvider, indexName, settings );
	}

	@Override
	public PutIndexMappingWork.Builder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		return PutIndexMappingWork.Builder.create( gsonProvider, indexName, mapping );
	}

	@Override
	public WaitForIndexStatusWork.Builder waitForIndexStatusWork(URLEncodedString indexName, IndexStatus requiredStatus,
			int requiredStatusTimeoutInMs) {
		return new WaitForIndexStatusWork.Builder( indexName, requiredStatus, requiredStatusTimeoutInMs );
	}

	@Override
	public PutIndexAliasesWork.Builder putIndexAliases(URLEncodedString indexName,
			Map<String, IndexAliasDefinition> aliases) {
		return new PutIndexAliasesWork.Builder( gsonProvider, indexName, aliases );
	}
}
