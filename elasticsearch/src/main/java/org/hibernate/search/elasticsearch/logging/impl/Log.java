/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import java.util.List;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.elasticsearch.client.impl.BulkRequestFailedException;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.ClassFormatter;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import com.google.gson.JsonElement;

import io.searchbox.client.JestResult;

/**
 * Hibernate Search log abstraction for the Elasticsearch integration.
 *
 * @author Gunnar Morling
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends org.hibernate.search.util.logging.impl.Log {

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 1,
			value = "Cannot execute query '%2$s', as targeted entity type '%1$s' is not mapped to an Elasticsearch index")
	SearchException cannotRunEsQueryTargetingEntityIndexedWithNonEsIndexManager(@FormatWith(ClassFormatter.class) Class<?> entityType, String query);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 2,
			value = "Lucene query '%1$s' cannot be transformed into equivalent Elasticsearch query" )
	SearchException cannotTransformLuceneQueryIntoEsQuery(Query query);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 3,
			value = "Lucene filter '%1$s' cannot be transformed into equivalent Elasticsearch query" )
	SearchException cannotTransformLuceneFilterIntoEsQuery(Filter filter);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 4,
			value = "The sort order RANGE_DEFINITION_ORDER cant not be sent used with Elasticsearch" )
	SearchException cannotSendRangeDefinitionOrderToElasticsearchBackend();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 5,
			value = "The SortType '%1$s' cannot be used with a null sort field name")
	SearchException cannotUseThisSortTypeWithNullSortFieldName(SortField.Type sortType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 6,
			value = "Empty phrase queries are not supported")
	SearchException cannotQueryOnEmptyPhraseQuery();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 7,
			value = "Elasticsearch request failed.\n Request:\n========\n%1$sResponse:\n=========\n%2$s"
	)
	SearchException elasticsearchRequestFailed(String request, String response, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 8,
			value = "Elasticsearch request failed.\n Request:\n========\n%1$sResponse:\n=========\n%2$s"
	)
	BulkRequestFailedException elasticsearchBulkRequestFailed(String request, String response, @Param List<BackendRequest<? extends JestResult>> erroneousItems);

	@LogMessage(level = Level.WARN)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 9,
			value = "Field '%2$s' in '%1$s' requires a remote analyzer reference (got '%3$s' instead). The analyzer will be ignored.")
	void analyzerIsNotRemote(@FormatWith(ClassFormatter.class) Class<?> entityType, String fieldName, AnalyzerReference analyzerReference);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 10,
			value = "Elasticsearch connection time-out; check the cluster status, it should be 'green';\n Request:\n========\n%1$sResponse:\n=========\n%2$s" )
	SearchException elasticsearchRequestTimeout(String request, String response);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 11,
			value = "Projection of non-JSON-primitive field values is not supported: '%1$s'")
	SearchException unsupportedProjectionOfNonJsonPrimitiveFields(JsonElement value);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 12,
			value = "Interrupted while waiting for requests to be processed."
	)
	SearchException interruptedWhileWaitingForRequestCompletion(@Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 13,
			value = "@Factory method does not return a Filter class or an ElasticsearchFilter class: %1$s.%2$s"
	)
	SearchException filterFactoryMethodReturnsUnsupportedType(String implementorName, String factoryMethodName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 14,
			value = "Unable to access @Factory method: %1$s.%2$s"
	)
	SearchException filterFactoryMethodInaccessible(String implementorName, String factoryMethodName, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 15,
			value = "Filter implementation does not implement the Filter interface or the ElasticsearchFilter interface: %1$s"
	)
	SearchException filterHasUnsupportedType(String actualClassName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 16,
			value = "TopDocs not available when using Elasticsearch"
	)
	UnsupportedOperationException documentExtractorTopDocsUnsupported();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 17,
			value = "Cannot use Lucene query with Elasticsearch"
	)
	UnsupportedOperationException hsQueryLuceneQueryUnsupported();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 18,
			value = "Unexpected numeric encoding type for field '%2$s': %1$s"
	)
	SearchException unexpectedNumericEncodingType(String fieldType, String fieldName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 19,
			value = "Cannot project field '%2$s' for entity %1$s: unknown field"
	)
	SearchException unknownFieldForProjection(String entityType, String fieldName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 20,
			value = "Could not create mapping for entity type %1$s"
	)
	SearchException elasticsearchMappingCreationFailed(String entityType, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 21,
			value = "Unexpected field type for field '%2$s': %1$s"
	)
	SearchException unexpectedFieldType(String fieldType, String fieldName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 22,
			value = "Unexpected index status string: '%1$s'. Specify one of 'green', 'yellow' or 'red'."
	)
	SearchException unexpectedIndexStatusString(String status);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 23,
			value = "Positive timeout value expected, but it was: %1$s"
	)
	SearchException negativeTimeoutValue(int timeout);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 24,
			value = "Index '%1$s' has status '%3$s', but it is expected to be '%2$s'."
	)
	SearchException unexpectedIndexStatus(String indexName, String expected, String actual);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 25,
			value = "With an Elasticsearch backend it is not possible to get a ReaderProvider or an IndexReader"
	)
	UnsupportedOperationException indexManagerReaderProviderUnsupported();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 26,
			value = "Faceting request of type %1$s not supported"
	)
	SearchException facetingRequestHasUnsupportedType(String facetingRequestType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 27,
			value = "The 'indexNullAs' property for field '%2$s' needs to represent a Boolean to match the field type of the index. "
					+ "Please change value from '%1$s' to represent a Boolean." )
	SearchException nullMarkerNeedsToRepresentABoolean(String proposedTokenValue, String fieldName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 28,
			value = "The 'indexNullAs' property for field '%2$s' needs to represent a Date to match the field type of the index. "
					+ "Please change value from '%1$s' to represent a Date." )
	SearchException nullMarkerNeedsToRepresentADate(String proposedTokenValue, String fieldName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 29,
			value = "Cannot use an offset ('from', 'firstResult') when scrolling through Elasticsearch results"
	)
	UnsupportedOperationException unsupportedOffsettedScrolling();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 30,
			value = "Cannot scroll backward through Elasticsearch results. Previously accessed index was %1$s, requested index is %2$s."
	)
	UnsupportedOperationException unsupportedBackwardTraversal(int lastRequestedIndex, int index);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 31,
			value = "Cannot scroll backward more than %1$s positions through Elasticsearch results. First index still in memory is %2$s, requested index is %3$s."
	)
	SearchException backtrackingWindowOverflow(int backtrackingLimit, int windowStartIndex, int requestedIndex);

	@LogMessage(level = Level.WARN)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 32,
			value = "@DynamicBoost is not supported with Elasticsearch. Ignoring boost strategy '%1$s' for entity '%2$s' (field path '%3$s')."
	)
	void unsupportedDynamicBoost(Class<?> boostStrategyType, Class<?> entityType, String fieldPath);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 33,
			value = "Validation failed for schema of index '%1$s'"
	)
	SearchException schemaValidationFailed(String indexName, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 34,
			value = "No mappings available from Elasticsearch for validation of index '%1$s'. Either the index hasn't been defined yet, or no type mappings have been defined on this index yet."
	)
	SearchException mappingsMissing(String indexName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 35,
			value = "Could not retrieve the mappings from Elasticsearch for validation"
	)
	SearchException elasticsearchMappingRetrievalForValidationFailed(@Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 36,
			value = "Missing mapping for entity type '%1$s'"
	)
	ElasticsearchSchemaValidationException mappingMissing(String mappingName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 37,
			value = "Invalid mapping '%1$s'"
	)

	ElasticsearchSchemaValidationException mappingInvalid(String mappingName, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 38,
			value = "Missing property mapping for property '%1$s'"
	)
	ElasticsearchSchemaValidationException mappingPropertyMissing(String propertyName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 39,
			value = "Invalid property '%1$s'"
	)
	ElasticsearchSchemaValidationException mappingPropertyInvalid(String propertyName, @Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 40,
			value = "Invalid value for attribute '%1$s'. Expected '%2$s', actual is '%3$s'"
	)
	ElasticsearchSchemaValidationException mappingInvalidAttributeValue(String string, Object expectedValue, Object actualValue);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 41,
			value = "The output format (the first format in the '%1$s' attribute) is invalid. Expected '%2$s', actual is '%3$s'"
	)
	ElasticsearchSchemaValidationException mappingInvalidOutputFormat(String string, String expectedValue, String actualValue);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 42,
			value = "Invalid formats for attribute '%1$s'. Every required formats must be in the list,"
			+ " though it's not required to provide them in the same order, and the list must not contain unexpected formats."
			+ " Expected '%2$s', actual is '%3$s', missing elements are '%4$s', unexpected elements are '%5$s'."
	)
	ElasticsearchSchemaValidationException mappingInvalidInputFormat(String string, List<String> expectedValue,
			List<String> actualValue, List<String> missingFormats, List<String> unexpectedFormats);

}
