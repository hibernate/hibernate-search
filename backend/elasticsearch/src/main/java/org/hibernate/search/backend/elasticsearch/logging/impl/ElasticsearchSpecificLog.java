/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = ElasticsearchSpecificLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ElasticsearchSpecificLog {
	String CATEGORY_NAME = "org.hibernate.search.backend.elasticsearch";

	ElasticsearchSpecificLog INSTANCE =
			LoggerFactory.make( ElasticsearchSpecificLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------


	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 176,
			value = "Cannot execute '%s' because Amazon OpenSearch Serverless does not support this operation."
					+ " Either avoid this operation or switch to another Elasticsearch/OpenSearch distribution.")
	SearchException cannotExecuteOperationOnAmazonOpenSearchServerless(String operation);

	@Message(id = ID_OFFSET + 177, value = "The targeted Elasticsearch cluster does not expose index status,"
			+ " so index status requirements cannot be enforced.")
	SearchException cannotRequireIndexStatus();

}
