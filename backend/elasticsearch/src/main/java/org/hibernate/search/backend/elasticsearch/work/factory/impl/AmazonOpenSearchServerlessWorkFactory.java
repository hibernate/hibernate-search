/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.factory.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchSpecificLog;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.CloseIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteByQueryWork;
import org.hibernate.search.backend.elasticsearch.work.impl.FlushWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ForceMergeWork;
import org.hibernate.search.backend.elasticsearch.work.impl.OpenIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.RefreshWork;
import org.hibernate.search.backend.elasticsearch.work.impl.WaitForIndexStatusWork;

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

	public AmazonOpenSearchServerlessWorkFactory(GsonProvider gsonProvider, Boolean ignoreShardFailures) {
		super( gsonProvider, ignoreShardFailures );
	}

	@Override
	public boolean isDeleteByQuerySupported() {
		return false;
	}

	@Override
	public DeleteByQueryWork.Builder deleteByQuery(URLEncodedString indexName, JsonObject payload) {
		throw ElasticsearchSpecificLog.INSTANCE.cannotExecuteOperationOnAmazonOpenSearchServerless( "deleteByQuery" );
	}

	@Override
	public boolean isFlushSupported() {
		return false;
	}

	@Override
	public FlushWork.Builder flush() {
		throw ElasticsearchSpecificLog.INSTANCE.cannotExecuteOperationOnAmazonOpenSearchServerless( "flush" );
	}

	@Override
	public boolean isRefreshSupported() {
		return false;
	}

	@Override
	public RefreshWork.Builder refresh() {
		throw ElasticsearchSpecificLog.INSTANCE.cannotExecuteOperationOnAmazonOpenSearchServerless( "refresh" );
	}

	@Override
	public boolean isMergeSegmentsSupported() {
		return false;
	}

	@Override
	public ForceMergeWork.Builder mergeSegments() {
		throw ElasticsearchSpecificLog.INSTANCE.cannotExecuteOperationOnAmazonOpenSearchServerless( "mergeSegments" );
	}

	@Override
	public OpenIndexWork.Builder openIndex(URLEncodedString indexName) {
		throw ElasticsearchSpecificLog.INSTANCE.cannotExecuteOperationOnAmazonOpenSearchServerless( "openIndex" );
	}

	@Override
	public CloseIndexWork.Builder closeIndex(URLEncodedString indexName) {
		throw ElasticsearchSpecificLog.INSTANCE.cannotExecuteOperationOnAmazonOpenSearchServerless( "closeIndex" );
	}

	@Override
	public WaitForIndexStatusWork.Builder waitForIndexStatus(URLEncodedString indexName, IndexStatus requiredStatus,
			int requiredStatusTimeoutInMs) {
		throw ElasticsearchSpecificLog.INSTANCE.cannotExecuteOperationOnAmazonOpenSearchServerless( "waitForIndexStatus" );
	}

	@Override
	public boolean isWaitForIndexStatusSupported() {
		return false;
	}
}
