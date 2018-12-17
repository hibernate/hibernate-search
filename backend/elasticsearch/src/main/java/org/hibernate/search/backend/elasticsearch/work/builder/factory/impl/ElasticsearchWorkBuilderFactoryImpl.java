/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
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
import org.hibernate.search.backend.elasticsearch.work.real.impl.ClearScrollWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.CloseIndexWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.DeleteByQueryWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.DeleteWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.DropIndexWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.ExplainWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.FlushWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.GetIndexSettingsWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.IndexExistsWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.IndexWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.OpenIndexWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.OptimizeWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.PutIndexSettingsWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.RefreshWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.ScrollWork;
import org.hibernate.search.backend.elasticsearch.work.real.impl.SearchWork;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchWorkBuilderFactoryImpl implements ElasticsearchWorkBuilderFactory {

	private final GsonProvider gsonProvider;

	public ElasticsearchWorkBuilderFactoryImpl(GsonProvider gsonProvider) {
		this.gsonProvider = gsonProvider;
	}

	@Override
	public IndexWorkBuilder index(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject document) {
		return new IndexWork.Builder( indexName, typeName, id, document );
	}

	@Override
	public DeleteWorkBuilder delete(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id) {
		return new DeleteWork.Builder( indexName, typeName, id );
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

	// TODO restore method => BulkWorkBuilder bulk(List<BulkableElasticsearchWork<?>> bulkableWorks);

	@Override
	public <T> SearchWorkBuilder search(JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return new SearchWork.Builder( payload, searchResultExtractor );
	}

	@Override
	public ExplainWorkBuilder explain(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject payload) {
		return new ExplainWork.Builder( indexName, typeName, id, payload );
	}

	@Override
	public <T> ScrollWorkBuilder scroll(String scrollId, String scrollTimeout, ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return new ScrollWork.Builder( scrollId, scrollTimeout, searchResultExtractor );
	}

	@Override
	public ClearScrollWorkBuilder clearScroll(String scrollId) {
		return new ClearScrollWork.Builder( scrollId );
	}

	@Override
	public CreateIndexWorkBuilder createIndex(URLEncodedString indexName) {
		return new CreateIndexWork.Builder( gsonProvider, indexName );
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

	// TODO restore method => GetIndexTypeMappingsWorkBuilder getIndexTypeMappings(URLEncodedString indexName);

	// TODO restore method => PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, URLEncodedString typeName, TypeMapping mapping);

	// TODO restore method => WaitForIndexStatusWorkBuilder waitForIndexStatusWork(URLEncodedString indexName, ElasticsearchIndexStatus requiredStatus, String timeout);

}
