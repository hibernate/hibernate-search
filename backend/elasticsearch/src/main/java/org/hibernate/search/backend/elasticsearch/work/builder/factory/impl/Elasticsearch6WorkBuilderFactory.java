/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
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
import org.hibernate.search.backend.elasticsearch.work.builder.impl.GetIndexSettingsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.GetIndexTypeMappingWorkBuilder;
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
import org.hibernate.search.backend.elasticsearch.work.impl.BulkWork;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.ClearScrollWork;
import org.hibernate.search.backend.elasticsearch.work.impl.CloseIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.CountWork;
import org.hibernate.search.backend.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteByQueryWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DropIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ExplainWork;
import org.hibernate.search.backend.elasticsearch.work.impl.FlushWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexSettingsWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexTypeMappingWork;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexExistsWork;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.OpenIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.OptimizeWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexSettingsWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexTypeMappingWork;
import org.hibernate.search.backend.elasticsearch.work.impl.RefreshWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ScrollWork;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.WaitForIndexStatusWork;

import com.google.gson.JsonObject;

public class Elasticsearch6WorkBuilderFactory implements ElasticsearchWorkBuilderFactory {

	// Exposed publicly for testing purposes
	public static final URLEncodedString TYPE_NAME = URLEncodedString.fromString( "typeName" );

	private final GsonProvider gsonProvider;

	public Elasticsearch6WorkBuilderFactory(GsonProvider gsonProvider) {
		this.gsonProvider = gsonProvider;
	}

	@Override
	public IndexWorkBuilder index(URLEncodedString indexName, URLEncodedString id, String routingKey, JsonObject document) {
		return new IndexWork.Builder( indexName, TYPE_NAME, id, routingKey, document );
	}

	@Override
	public DeleteWorkBuilder delete(URLEncodedString indexName, URLEncodedString id, String routingKey) {
		return new DeleteWork.Builder( indexName, TYPE_NAME, id, routingKey );
	}

	@Override
	public DeleteByQueryWorkBuilder deleteByQuery(URLEncodedString indexName, JsonObject payload) {
		return new DeleteByQueryWork.Builder( indexName, payload, this );
	}

	@Override
	public FlushWorkBuilder flush() {
		return new FlushWork.Builder( this );
	}

	@Override
	public RefreshWorkBuilder refresh() {
		return new RefreshWork.Builder();
	}

	@Override
	public OptimizeWorkBuilder optimize() {
		return new OptimizeWork.Builder();
	}

	@Override
	public BulkWorkBuilder bulk(List<? extends BulkableElasticsearchWork<?>> bulkableWorks) {
		return new BulkWork.Builder( bulkableWorks );
	}

	@Override
	public <T> SearchWorkBuilder<T> search(JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return new SearchWork.Builder<>( payload, searchResultExtractor );
	}

	@Override
	public CountWorkBuilder count(Set<URLEncodedString> indexNames) {
		return new CountWork.Builder( indexNames );
	}

	@Override
	public ExplainWorkBuilder explain(URLEncodedString indexName, URLEncodedString id, JsonObject payload) {
		return new ExplainWork.Builder( indexName, TYPE_NAME, id, payload );
	}

	@Override
	public <T> ScrollWorkBuilder<T> scroll(String scrollId, String scrollTimeout, ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return new ScrollWork.Builder<>( scrollId, scrollTimeout, searchResultExtractor );
	}

	@Override
	public ClearScrollWorkBuilder clearScroll(String scrollId) {
		return new ClearScrollWork.Builder( scrollId );
	}

	@Override
	public CreateIndexWorkBuilder createIndex(URLEncodedString indexName) {
		return new CreateIndexWork.Builder( gsonProvider, indexName, TYPE_NAME );
	}

	@Override
	public DropIndexWorkBuilder dropIndex(URLEncodedString indexName) {
		return new DropIndexWork.Builder( indexName );
	}

	@Override
	public OpenIndexWorkBuilder openIndex(URLEncodedString indexName) {
		return new OpenIndexWork.Builder( indexName );
	}

	@Override
	public CloseIndexWorkBuilder closeIndex(URLEncodedString indexName) {
		return new CloseIndexWork.Builder( indexName );
	}

	@Override
	public IndexExistsWorkBuilder indexExists(URLEncodedString indexName) {
		return new IndexExistsWork.Builder( indexName );
	}

	@Override
	public GetIndexSettingsWorkBuilder getIndexSettings(URLEncodedString indexName) {
		return new GetIndexSettingsWork.Builder( indexName );
	}

	@Override
	public PutIndexSettingsWorkBuilder putIndexSettings(URLEncodedString indexName, IndexSettings settings) {
		return new PutIndexSettingsWork.Builder( gsonProvider, indexName, settings );
	}

	@Override
	public GetIndexTypeMappingWorkBuilder getIndexTypeMapping(URLEncodedString indexName) {
		return new GetIndexTypeMappingWork.Builder( indexName, TYPE_NAME );
	}

	@Override
	public PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		return new PutIndexTypeMappingWork.Builder( gsonProvider, indexName, TYPE_NAME, mapping );
	}

	@Override
	public WaitForIndexStatusWorkBuilder waitForIndexStatusWork(URLEncodedString indexName, ElasticsearchIndexStatus requiredStatus, String timeout) {
		return new WaitForIndexStatusWork.Builder( indexName, requiredStatus, timeout );
	}

}
