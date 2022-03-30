/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.link.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

/**
 * Represent the "link" to an Elasticsearch cluster,
 * with all the relevant components:
 * the client, configured to target the URL pointing to the cluster,
 * and several components that are configured to be compatible with the version of Elasticsearch running on the cluster.
 */
public interface ElasticsearchLink {

	ElasticsearchClient getClient();

	GsonProvider getGsonProvider();

	ElasticsearchSearchSyntax getSearchSyntax();

	ElasticsearchIndexMetadataSyntax getIndexMetadataSyntax();

	ElasticsearchWorkFactory getWorkFactory();

	ElasticsearchSearchResultExtractorFactory getSearchResultExtractorFactory();

	Integer getScrollTimeout();

}
