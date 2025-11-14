/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.jdk.logging.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = ElasticsearchClientLog.CATEGORY_NAME,
		description = """
				Logs information on low-level Elasticsearch backend operations.
				+
				This may include warnings about misconfigured Elasticsearch REST clients or index operations.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ElasticsearchClientLog {
	String CATEGORY_NAME = "org.hibernate.search.elasticsearch.client";

	ElasticsearchClientLog INSTANCE = LoggerFactory.make( ElasticsearchClientLog.class, CATEGORY_NAME, MethodHandles.lookup() );

}
