/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET_LEGACY_ES;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import org.apache.http.HttpHost;

@CategorizedLogger(
		category = ElasticsearchRequestLog.CATEGORY_NAME,
		description = """
				Logs executed requests and responses sent to the Elasticsearch cluster.
				It also includes the execution time of the request.
				+
				Logger must be enabled at TRACE level for this category to print messages.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ElasticsearchRequestLog extends BasicLogger {
	/**
	 * This is the category of the Logger used to print out executed Elasticsearch requests,
	 * along with the execution time.
	 * <p>
	 * To enable the logger, the category needs to be enabled at TRACE level.
	 */
	String CATEGORY_NAME = "org.hibernate.search.backend.elasticsearch.request";

	ElasticsearchRequestLog INSTANCE =
			LoggerFactory.make( ElasticsearchRequestLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET_LEGACY_ES + 82,
			value = "Executed Elasticsearch HTTP %s request to '%s' with path '%s',"
					+ " query parameters %s and %d objects in payload in %dms."
					+ " Response had status %d '%s'. Request body: <%s>. Response body: <%s>")
	void executedRequestWithFailure(String method, HttpHost host, String path, Map<String, String> getParameters,
			int bodyParts, long timeInMs,
			int responseStatusCode, String responseStatusMessage,
			String requestBodyParts, String responseBody);

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET_LEGACY_ES + 93,
			value = "Executed Elasticsearch HTTP %s request to '%s' with path '%s',"
					+ " query parameters %s and %d objects in payload in %dms."
					+ " Response had status %d '%s'. Request body: <%s>. Response body: <%s>")
	void executedRequest(String method, HttpHost host, String path, Map<String, String> getParameters, int bodyParts,
			long timeInMs,
			int responseStatusCode, String responseStatusMessage,
			String requestBodyParts, String responseBody);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------


}
