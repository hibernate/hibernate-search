/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
