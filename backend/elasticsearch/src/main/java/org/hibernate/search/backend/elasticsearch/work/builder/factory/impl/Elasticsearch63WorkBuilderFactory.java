/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexMetadataWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexMappingWork;

/**
 * A work builder factory for ES6.3 to ES6.6.
 * <p>
 * Compared to ES6.7:
 * <ul>
 *     <li>We do NOT set an "include_type_name=true" parameter in index creation and mapping APIs</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // We use Paths.DOC on purpose
public class Elasticsearch63WorkBuilderFactory extends Elasticsearch67WorkBuilderFactory {

	public Elasticsearch63WorkBuilderFactory(GsonProvider gsonProvider) {
		super( gsonProvider );
	}

	@Override
	public CreateIndexWork.Builder createIndex(URLEncodedString indexName) {
		return CreateIndexWork.Builder.forElasticsearch66AndBelow( gsonProvider, indexName, Paths.DOC );
	}

	@Override
	public GetIndexMetadataWork.Builder getIndexMetadata() {
		return GetIndexMetadataWork.Builder.forElasticsearch66AndBelow( Paths.DOC );
	}

	@Override
	public PutIndexMappingWork.Builder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		return PutIndexMappingWork.Builder.forElasticsearch66AndBelow( gsonProvider, indexName, Paths.DOC, mapping );
	}
}
