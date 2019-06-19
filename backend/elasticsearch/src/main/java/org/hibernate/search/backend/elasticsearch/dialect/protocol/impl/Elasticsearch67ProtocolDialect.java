/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.protocol.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.search.query.impl.Elasticsearch6SearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.Elasticsearch67WorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;

/**
 * The protocol dialect for Elasticsearch 6.7 and later 6.x.
 */
public class Elasticsearch67ProtocolDialect extends Elasticsearch70ProtocolDialect
		implements ElasticsearchProtocolDialect {

	@Override
	public ElasticsearchWorkBuilderFactory createWorkBuilderFactory(GsonProvider gsonProvider) {
		// Necessary because of the breaking changes related to type names in ES7
		return new Elasticsearch67WorkBuilderFactory( gsonProvider );
	}

	@Override
	public ElasticsearchSearchResultExtractorFactory createSearchResultExtractorFactory() {
		// Necessary because the total hit count is formatted differently in ES7
		return new Elasticsearch6SearchResultExtractorFactory();
	}
}
