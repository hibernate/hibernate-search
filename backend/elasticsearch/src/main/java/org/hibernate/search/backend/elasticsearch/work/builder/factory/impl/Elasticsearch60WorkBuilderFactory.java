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
import org.hibernate.search.backend.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexTypeMappingWork;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexExistsWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexTypeMappingWork;

/**
 * A work builder factory for ES6.0 to ES6.6.
 * <p>
 * Compared to ES6.7:
 * <ul>
 *     <li>We do NOT set an "include_type_name=true" parameter in index creation and mapping APIs</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // We use Paths.DOC on purpose
public class Elasticsearch60WorkBuilderFactory extends Elasticsearch67WorkBuilderFactory {

	public Elasticsearch60WorkBuilderFactory(GsonProvider gsonProvider) {
		super( gsonProvider );
	}

	@Override
	public CreateIndexWorkBuilder createIndex(URLEncodedString indexName) {
		return CreateIndexWork.Builder.forElasticsearch66AndBelow( gsonProvider, indexName, Paths.DOC );
	}

	@Override
	public IndexExistsWorkBuilder indexExists(URLEncodedString indexName) {
		return IndexExistsWork.Builder.forElasticsearch66AndBelow( indexName );
	}

	@Override
	public GetIndexTypeMappingWorkBuilder getIndexTypeMapping(URLEncodedString indexName) {
		return GetIndexTypeMappingWork.Builder.forElasticsearch66AndBelow( indexName, Paths.DOC );
	}

	@Override
	public PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		return PutIndexTypeMappingWork.Builder.forElasticsearch66AndBelow( gsonProvider, indexName, Paths.DOC, mapping );
	}
}
