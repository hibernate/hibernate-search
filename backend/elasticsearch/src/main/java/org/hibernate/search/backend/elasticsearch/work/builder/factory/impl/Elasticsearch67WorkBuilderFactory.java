/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CreateIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.GetIndexTypeMappingWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexExistsWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexMappingWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexTypeMappingWork;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexExistsWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexTypeMappingWork;

import com.google.gson.JsonObject;

/**
 * A work builder factory for ES6.
 * <p>
 * Compared to ES7:
 * <ul>
 *     <li>Mappings are assigned a "type name"; we use the hardcoded "doc" type name</li>
 *     <li>Some URLs require to include this type name instead of the "_doc" keyword used in ES7.</li>
 *     <li>We set an "include_type_name=true" parameter in index creation and mapping APIs</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // We use Paths.DOC on purpose
public class Elasticsearch67WorkBuilderFactory extends Elasticsearch7WorkBuilderFactory {

	public Elasticsearch67WorkBuilderFactory(GsonProvider gsonProvider) {
		super( gsonProvider );
	}

	@Override
	public <T> SearchWorkBuilder<T> search(JsonObject payload,
			ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return ElasticsearchSearchWork.Builder.forElasticsearch6AndBelow( payload, searchResultExtractor );
	}

	@Override
	public CreateIndexWorkBuilder createIndex(URLEncodedString indexName) {
		return CreateIndexWork.Builder.forElasticsearch67( gsonProvider, indexName, Paths.DOC );
	}

	@Override
	public IndexExistsWorkBuilder indexExists(URLEncodedString indexName) {
		return IndexExistsWork.Builder.forElasticsearch67( indexName );
	}

	@Override
	public GetIndexTypeMappingWorkBuilder getIndexTypeMapping(URLEncodedString indexName) {
		return GetIndexTypeMappingWork.Builder.forElasticsearch67( indexName, Paths.DOC );
	}

	@Override
	public PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		return PutIndexTypeMappingWork.Builder.forElasticsearch67( gsonProvider, indexName, Paths.DOC, mapping );
	}

	@Override
	protected URLEncodedString getTypeKeyword() {
		return Paths.DOC;
	}
}
