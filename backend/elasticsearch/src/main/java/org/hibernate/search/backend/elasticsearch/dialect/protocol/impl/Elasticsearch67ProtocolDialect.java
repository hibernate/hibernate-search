/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.protocol.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.Elasticsearch64IndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.Elasticsearch64SearchSyntax;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.query.impl.Elasticsearch56SearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.Elasticsearch67WorkFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

/**
 * The protocol dialect for Elasticsearch 6.7 and later 6.x.
 */
public class Elasticsearch67ProtocolDialect implements ElasticsearchProtocolDialect {

	@Override
	public ElasticsearchIndexMetadataSyntax createIndexMetadataSyntax() {
		return new Elasticsearch64IndexMetadataSyntax();
	}

	@Override
	public ElasticsearchSearchSyntax createSearchSyntax() {
		return new Elasticsearch64SearchSyntax();
	}

	@Override
	public ElasticsearchWorkFactory createWorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures) {
		return new Elasticsearch67WorkFactory( gsonProvider, ignoreShardFailures );
	}

	@Override
	public ElasticsearchSearchResultExtractorFactory createSearchResultExtractorFactory() {
		return new Elasticsearch56SearchResultExtractorFactory();
	}
}
