package org.hibernate.search.backend.elasticsearch.link.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClient;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

/**
 * Represent the "link" to an Elasticsearch cluster,
 * with all the relevant components:
 * the client, configured to target the URL pointing to the cluster,
 * and several components that are configured to be compatible with the version of Elasticsearch running on the cluster.
 */
public interface ElasticsearchLink {

	ElasticsearchJsonClient getClient();

	JsonProvider getJsonProvider();

	ElasticsearchSearchSyntax getSearchSyntax();

	ElasticsearchIndexMetadataSyntax getIndexMetadataSyntax();

	ElasticsearchWorkFactory getWorkFactory();

	ElasticsearchSearchResultExtractorFactory getSearchResultExtractorFactory();

	Integer getScrollTimeout();

	IndexLayoutStrategy getIndexLayoutStrategy();

	TypeNameMapping getTypeNameMapping();

	IndexNames createIndexNames(String hibernateSearchIndexName, String mappedTypeName);

	SearchProjectionBackendContext getSearchProjectionBackendContext();
}
