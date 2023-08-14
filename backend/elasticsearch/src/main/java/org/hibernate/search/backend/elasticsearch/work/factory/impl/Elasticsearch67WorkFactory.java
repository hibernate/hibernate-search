/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.factory.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.ExplainWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexMetadataWork;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexMappingWork;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchWork;

import com.google.gson.JsonObject;

/**
 * A work builder factory for ES6.7 and later 6.x.
 * <p>
 * Compared to ES7:
 * <ul>
 *     <li>Mappings are assigned a "type name"; we use the hardcoded "doc" type name</li>
 *     <li>Some URLs require to include this type name instead of the "_doc" keyword used in ES7.</li>
 *     <li>We set an "include_type_name=true" parameter in index creation and mapping APIs</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // We use Paths.DOC on purpose
public class Elasticsearch67WorkFactory extends Elasticsearch7WorkFactory {

	public Elasticsearch67WorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures) {
		super( gsonProvider, ignoreShardFailures );
	}


	@Override
	public IndexWork.Builder index(String entityTypeName, Object entityIdentifier,
			URLEncodedString elasticsearchIndexName,
			String documentIdentifier, String routingKey, JsonObject document) {
		return IndexWork.Builder.forElasticsearch67AndBelow( entityTypeName, entityIdentifier,
				elasticsearchIndexName, Paths.DOC, documentIdentifier, routingKey, document );
	}

	@Override
	public DeleteWork.Builder delete(String entityTypeName, Object entityIdentifier,
			URLEncodedString elasticsearchIndexName, String documentIdentifier, String routingKey) {
		return DeleteWork.Builder.forElasticsearch67AndBelow( entityTypeName, entityIdentifier,
				elasticsearchIndexName, Paths.DOC, documentIdentifier, routingKey );
	}

	@Override
	public <T> SearchWork.Builder<T> search(JsonObject payload,
			ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		SearchWork.Builder<T> builder = SearchWork.Builder.forElasticsearch63to68( payload, searchResultExtractor );
		if ( ignoreShardFailures ) {
			builder.ignoreShardFailures();
		}
		return builder;
	}

	@Override
	public ExplainWork.Builder explain(URLEncodedString indexName, URLEncodedString id, JsonObject payload) {
		return ExplainWork.Builder.forElasticsearch67AndBelow( indexName, Paths.DOC, id, payload );
	}

	@Override
	public CreateIndexWork.Builder createIndex(URLEncodedString indexName) {
		return CreateIndexWork.Builder.forElasticsearch67( gsonProvider, indexName, Paths.DOC );
	}

	@Override
	public GetIndexMetadataWork.Builder getIndexMetadata() {
		return GetIndexMetadataWork.Builder.forElasticsearch67( Paths.DOC );
	}

	@Override
	public PutIndexMappingWork.Builder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		return PutIndexMappingWork.Builder.forElasticsearch67( gsonProvider, indexName, Paths.DOC, mapping );
	}
}
