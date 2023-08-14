/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.protocol.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.Elasticsearch56IndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.Elasticsearch60SearchSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.query.impl.Elasticsearch56SearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.Elasticsearch56WorkFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

/**
 * The protocol dialect for Elasticsearch 6.0 to 6.2.
 */
public class Elasticsearch60ProtocolDialect implements ElasticsearchProtocolDialect {

	@Override
	public ElasticsearchIndexMetadataSyntax createIndexMetadataSyntax() {
		return new Elasticsearch56IndexMetadataSyntax();
	}

	@Override
	public ElasticsearchSearchSyntax createSearchSyntax() {
		return new Elasticsearch60SearchSyntax();
	}

	@Override
	public ElasticsearchWorkFactory createWorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures) {
		return new Elasticsearch56WorkFactory( gsonProvider, ignoreShardFailures );
	}

	@Override
	public ElasticsearchSearchResultExtractorFactory createSearchResultExtractorFactory() {
		return new Elasticsearch56SearchResultExtractorFactory();
	}
}
