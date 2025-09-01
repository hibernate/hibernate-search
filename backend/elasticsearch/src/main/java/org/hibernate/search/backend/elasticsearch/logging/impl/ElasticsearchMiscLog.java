/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;


import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.DEBUG;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = ElasticsearchMiscLog.CATEGORY_NAME,
		description = """
				The main category for the Elasticsearch backend-specific logs.
				It may also include logs that do not fit any other, more specific, Elasticsearch category.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ElasticsearchMiscLog {
	String CATEGORY_NAME = "org.hibernate.search.elasticsearch";

	ElasticsearchMiscLog INSTANCE = LoggerFactory.make( ElasticsearchMiscLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 6,
			value = "Invalid target for Elasticsearch extension: '%1$s'."
					+ " This extension can only be applied to components created by an Elasticsearch backend.")
	SearchException elasticsearchExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET + 19,
			value = "Invalid requested type for this backend: '%1$s'."
					+ " Elasticsearch backends can only be unwrapped to '%2$s'.")
	SearchException backendUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 33,
			value = "Invalid requested type for this index manager: '%1$s'."
					+ " Elasticsearch index managers can only be unwrapped to '%2$s'.")
	SearchException indexManagerUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 157, value = "Unable to export the schema for '%1$s' index: %2$s")
	SearchException unableToExportSchema(String indexName, String message, @Cause IOException e);

	@Message(id = ID_OFFSET + 176,
			value = "Cannot execute '%s' because Amazon OpenSearch Serverless does not support this operation."
					+ " Either avoid this operation or switch to another Elasticsearch/OpenSearch distribution.")
	SearchException cannotExecuteOperationOnAmazonOpenSearchServerless(String operation);

	@Message(id = ID_OFFSET + 177, value = "The targeted Elasticsearch cluster does not expose index status,"
			+ " so index status requirements cannot be enforced.")
	SearchException cannotRequireIndexStatus();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 191, value = "Normalizing index name from '%1$s' to '%2$s'")
	void normalizeIndexName(String indexName, String esIndexName);

}
