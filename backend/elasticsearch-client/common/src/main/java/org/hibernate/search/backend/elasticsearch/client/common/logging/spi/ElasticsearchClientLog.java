/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.logging.spi;

import static org.hibernate.search.backend.elasticsearch.client.common.logging.spi.ElasticsearchClientCommonLog.ID_BACKEND_OFFSET;
import static org.hibernate.search.backend.elasticsearch.client.common.logging.spi.ElasticsearchClientCommonLog.ID_OFFSET_LEGACY_ES;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.DurationInSecondsAndFractionsFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import com.google.gson.JsonObject;

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

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	@Message(id = ID_OFFSET_LEGACY_ES + 7,
			value = "Elasticsearch request failed: %3$s\nRequest: %1$s\nResponse: %2$s")
	SearchException elasticsearchRequestFailed(
			@FormatWith(ElasticsearchRequestFormatter.class) ElasticsearchRequest request,
			@FormatWith(ElasticsearchResponseFormatter.class) ElasticsearchResponse response,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 8,
			// Note: no need to add a '\n' before "Response", since the formatter will always add one
			value = "Elasticsearch bulked request failed: %3$s\nRequest metadata: %1$sResponse: %2$s")
	SearchException elasticsearchBulkedRequestFailed(
			@FormatWith(ElasticsearchJsonObjectFormatter.class) JsonObject requestMetadata,
			@FormatWith(ElasticsearchJsonObjectFormatter.class) JsonObject response,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 10,
			value = "Elasticsearch response indicates a timeout (HTTP status 408)")
	SearchException elasticsearchStatus408RequestTimeout();

	@Message(id = ID_OFFSET_LEGACY_ES + 20,
			value = "Unable to update mapping for index '%1$s': %2$s")
	SearchException elasticsearchMappingUpdateFailed(String indexName, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 24, value = "Index '%1$s' failed to reach status '%2$s' after %3$sms.")
	SearchException unexpectedIndexStatus(URLEncodedString indexName, String expected, int requiredStatusTimeoutInMs,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 34,
			value = "Unable to retrieve index metadata from Elasticsearch: %1$s")
	SearchException elasticsearchIndexMetadataRetrievalFailed(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 35,
			value = "Unable to update schema for index '%1$s': %2$s")
	SearchException schemaUpdateFailed(URLEncodedString indexName, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 50,
			value = "Missing index: index names [%1$s, %2$s] do not point to any index in the Elasticsearch cluster.")
	SearchException indexMissing(URLEncodedString write, URLEncodedString read);

	@Message(id = ID_OFFSET_LEGACY_ES + 67,
			value = "Unable to update settings for index '%1$s': %2$s")
	SearchException elasticsearchSettingsUpdateFailed(Object indexName, String causeMessage, @Cause Exception cause);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET_LEGACY_ES + 69,
			value = "Closed Elasticsearch index '%1$s' automatically.")
	void closedIndex(Object indexName);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET_LEGACY_ES + 70,
			value = "Opened Elasticsearch index '%1$s' automatically.")
	void openedIndex(Object indexName);

	@Message(id = ID_OFFSET_LEGACY_ES + 89,
			value = "Unable to parse Elasticsearch response. Status code was '%1$d', status phrase was '%2$s'."
					+ " Nested exception: %3$s")
	SearchException failedToParseElasticsearchResponse(int statusCode, String statusPhrase,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 90,
			value = "Elasticsearch response indicates a failure.")
	SearchException elasticsearchResponseIndicatesFailure();

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_BACKEND_OFFSET + 18,
			value = "Invalid requested type for client: '%1$s'."
					+ " The Elasticsearch low-level client can only be unwrapped to '%2$s'.")
	SearchException clientUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass);

	@Message(id = ID_BACKEND_OFFSET + 25,
			value = "Invalid field reference for this document element:"
					+ " this document element has path '%1$s', but the referenced field has a parent with path '%2$s'.")
	SearchException invalidFieldForDocumentElement(String expectedPath, String actualPath);

	@Message(id = ID_BACKEND_OFFSET + 26,
			value = "Missing data in the Elasticsearch response.")
	AssertionFailure elasticsearchResponseMissingData();

	@Message(id = ID_BACKEND_OFFSET + 31,
			value = "Unable to resolve index name '%1$s' to an entity type: %2$s")
	SearchException elasticsearchResponseUnknownIndexName(String elasticsearchIndexName, String causeMessage,
			@Cause Exception e);

	@Message(id = ID_BACKEND_OFFSET + 44, value = "Unable to shut down the Elasticsearch client: %1$s")
	SearchException unableToShutdownClient(String causeMessage, @Cause Exception cause);

	@Message(id = ID_BACKEND_OFFSET + 88, value = "Call to the bulk REST API failed: %1$s")
	SearchException elasticsearchFailedBecauseOfBulkFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_BACKEND_OFFSET + 90, value = "Request execution exceeded the timeout of %1$s. Request was %2$s")
	SearchTimeoutException requestTimedOut(@FormatWith(DurationInSecondsAndFractionsFormatter.class) Duration timeout,
			@FormatWith(ElasticsearchRequestFormatter.class) ElasticsearchRequest request);

	@Message(id = ID_BACKEND_OFFSET + 93,
			value = "Invalid Elasticsearch index layout:"
					+ " index names [%1$s, %2$s] resolve to multiple distinct indexes %3$s."
					+ " These names must resolve to a single index.")
	SearchException elasticsearchIndexNameAndAliasesMatchMultipleIndexes(URLEncodedString write, URLEncodedString read,
			Set<String> matchingIndexes);

	@Message(id = ID_BACKEND_OFFSET + 94,
			value = "Invalid Elasticsearch index layout:"
					+ " primary (non-alias) name for existing Elasticsearch index '%1$s'"
					+ " does not match the expected pattern '%2$s'.")
	SearchException invalidIndexPrimaryName(String elasticsearchIndexName, Pattern pattern);

	@Message(id = ID_BACKEND_OFFSET + 95,
			value = "Invalid Elasticsearch index layout:"
					+ " unique key '%1$s' extracted from the index name does not match any of %2$s.")
	SearchException invalidIndexUniqueKey(String uniqueKey, Set<String> knownKeys);

	@Message(id = ID_BACKEND_OFFSET + 125,
			value = "Unable to update aliases for index '%1$s': %2$s")
	SearchException elasticsearchAliasUpdateFailed(Object indexName, String causeMessage, @Cause Exception cause);


}
