/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.factory.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.CloseIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteByQueryWork;
import org.hibernate.search.backend.elasticsearch.work.impl.FlushWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ForceMergeWork;
import org.hibernate.search.backend.elasticsearch.work.impl.OpenIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.RefreshWork;
import org.hibernate.search.backend.elasticsearch.work.impl.WaitForIndexStatusWork;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * A work builder factory for Amazon OpenSearch Serverless.
 * <p>
 * Not all operations are supported,
 * see <a href="https://docs.aws.amazon.com/opensearch-service/latest/developerguide/serverless-genref.html#serverless-operations">the documentation</a>.
 *
 * @see org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName#AMAZON_OPENSEARCH_SERVERLESS
 */
public class AmazonOpenSearchServerlessWorkFactory extends Elasticsearch7WorkFactory {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public AmazonOpenSearchServerlessWorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures) {
		super( gsonProvider, ignoreShardFailures );
	}

	@Override
	public boolean isDeleteByQuerySupported() {
		return false;
	}

	@Override
	public DeleteByQueryWork.Builder deleteByQuery(URLEncodedString indexName, JsonObject payload) {
		throw log.cannotExecuteOperationOnAmazonOpenSearchServerless( "deleteByQuery" );
	}

	@Override
	public boolean isFlushSupported() {
		return false;
	}

	@Override
	public FlushWork.Builder flush() {
		throw log.cannotExecuteOperationOnAmazonOpenSearchServerless( "flush" );
	}

	@Override
	public boolean isRefreshSupported() {
		return false;
	}

	@Override
	public RefreshWork.Builder refresh() {
		throw log.cannotExecuteOperationOnAmazonOpenSearchServerless( "refresh" );
	}

	@Override
	public boolean isMergeSegmentsSupported() {
		return false;
	}

	@Override
	public ForceMergeWork.Builder mergeSegments() {
		throw log.cannotExecuteOperationOnAmazonOpenSearchServerless( "mergeSegments" );
	}

	@Override
	public OpenIndexWork.Builder openIndex(URLEncodedString indexName) {
		throw log.cannotExecuteOperationOnAmazonOpenSearchServerless( "openIndex" );
	}

	@Override
	public CloseIndexWork.Builder closeIndex(URLEncodedString indexName) {
		throw log.cannotExecuteOperationOnAmazonOpenSearchServerless( "closeIndex" );
	}

	@Override
	public WaitForIndexStatusWork.Builder waitForIndexStatus(URLEncodedString indexName, IndexStatus requiredStatus,
			int requiredStatusTimeoutInMs) {
		throw log.cannotExecuteOperationOnAmazonOpenSearchServerless( "waitForIndexStatus" );
	}

	@Override
	public boolean isWaitForIndexStatusSupported() {
		return false;
	}
}
