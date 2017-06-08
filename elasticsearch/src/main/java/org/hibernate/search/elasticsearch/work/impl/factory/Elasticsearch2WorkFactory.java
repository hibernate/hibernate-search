/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl.factory;

import java.util.List;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.work.impl.BulkWork;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ClearScrollWork;
import org.hibernate.search.elasticsearch.work.impl.CloseIndexWork;
import org.hibernate.search.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.elasticsearch.work.impl.ES2DeleteByQueryWork;
import org.hibernate.search.elasticsearch.work.impl.DeleteWork;
import org.hibernate.search.elasticsearch.work.impl.DropIndexWork;
import org.hibernate.search.elasticsearch.work.impl.ExplainWork;
import org.hibernate.search.elasticsearch.work.impl.ES2FlushWork;
import org.hibernate.search.elasticsearch.work.impl.GetIndexSettingsWork;
import org.hibernate.search.elasticsearch.work.impl.GetIndexTypeMappingsWork;
import org.hibernate.search.elasticsearch.work.impl.IndexExistsWork;
import org.hibernate.search.elasticsearch.work.impl.IndexWork;
import org.hibernate.search.elasticsearch.work.impl.OpenIndexWork;
import org.hibernate.search.elasticsearch.work.impl.ES2OptimizeWork;
import org.hibernate.search.elasticsearch.work.impl.PutIndexSettingsWork;
import org.hibernate.search.elasticsearch.work.impl.PutIndexTypeMappingWork;
import org.hibernate.search.elasticsearch.work.impl.RefreshWork;
import org.hibernate.search.elasticsearch.work.impl.ScrollWork;
import org.hibernate.search.elasticsearch.work.impl.SearchWork;
import org.hibernate.search.elasticsearch.work.impl.WaitForIndexStatusWork;
import org.hibernate.search.elasticsearch.work.impl.builder.BulkWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.ClearScrollWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.CloseIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.CreateIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.DropIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.ExplainWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.FlushWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.GetIndexSettingsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.GetIndexTypeMappingsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.IndexExistsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.IndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.OpenIndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.OptimizeWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.PutIndexMappingWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.PutIndexSettingsWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.RefreshWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.ScrollWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.SearchWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.WaitForIndexStatusWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch2WorkFactory implements ElasticsearchWorkFactory {

	private final GsonProvider gsonProvider;

	public Elasticsearch2WorkFactory(GsonProvider gsonProvider) {
		super();
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
		return new ES2DeleteByQueryWork.Builder( indexName, payload, this );
	}

	@Override
	public FlushWorkBuilder flush() {
		return new ES2FlushWork.Builder();
	}

	@Override
	public RefreshWorkBuilder refresh() {
		return new RefreshWork.Builder();
	}

	@Override
	public OptimizeWorkBuilder optimize() {
		return new ES2OptimizeWork.Builder();
	}

	@Override
	public BulkWorkBuilder bulk(List<BulkableElasticsearchWork<?>> bulkableWorks) {
		return new BulkWork.Builder( bulkableWorks );
	}

	@Override
	public SearchWorkBuilder search(JsonObject payload) {
		return new SearchWork.Builder( payload );
	}

	@Override
	public ExplainWorkBuilder explain(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject payload) {
		return new ExplainWork.Builder( indexName, typeName, id, payload );
	}

	@Override
	public ScrollWorkBuilder scroll(String scrollId, String scrollTimeout) {
		return new ScrollWork.Builder( scrollId, scrollTimeout );
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

	@Override
	public GetIndexTypeMappingsWorkBuilder getIndexTypeMappings(URLEncodedString indexName) {
		return new GetIndexTypeMappingsWork.Builder( indexName );
	}

	@Override
	public PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, URLEncodedString typeName, TypeMapping mapping) {
		return new PutIndexTypeMappingWork.Builder( gsonProvider, indexName, typeName, mapping );
	}

	@Override
	public WaitForIndexStatusWorkBuilder waitForIndexStatusWork(URLEncodedString indexName, ElasticsearchIndexStatus requiredStatus, String timeout) {
		return new WaitForIndexStatusWork.Builder( indexName, requiredStatus, timeout );
	}
}
