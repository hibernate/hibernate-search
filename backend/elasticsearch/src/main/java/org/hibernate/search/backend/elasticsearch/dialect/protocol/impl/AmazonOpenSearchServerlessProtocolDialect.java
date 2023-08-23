/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.protocol.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.AmazonOpenSearchServerlessWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The model dialect for Amazon OpenSearch Serverless.
 *
 * @see org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName#AMAZON_OPENSEARCH_SERVERLESS
 */
@Incubating
public class AmazonOpenSearchServerlessProtocolDialect extends Elasticsearch70ProtocolDialect {

	@Override
	public ElasticsearchWorkFactory createWorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures) {
		return new AmazonOpenSearchServerlessWorkFactory( gsonProvider, ignoreShardFailures );
	}
}
