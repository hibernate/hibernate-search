package org.hibernate.search.backend.elasticsearch.client.rest4.logging.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import org.apache.http.ContentTooLongException;

@CategorizedLogger(
		category = ElasticsearchClientLog.CATEGORY_NAME,
		description = """
				Logs information on low-level Elasticsearch backend operations.
				+
				This may include warnings about misconfigured Elasticsearch REST clients or index operations.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRange(min = MessageConstants.BACKEND_ES_CLIENT_ID_RANGE_MIN + 0,
		max = MessageConstants.BACKEND_ES_CLIENT_ID_RANGE_MIN + 999)
public interface ElasticsearchClientLog {
	String CATEGORY_NAME = "org.hibernate.search.elasticsearch.client";

	ElasticsearchClientLog INSTANCE = LoggerFactory.make( ElasticsearchClientLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = MessageConstants.BACKEND_ES_CLIENT_ID_RANGE_MIN + 0, value = "Response input exceeds byte limit %s")
	ContentTooLongException responseInputLimitExceeded(long limit, @Cause RuntimeException cause);
}
