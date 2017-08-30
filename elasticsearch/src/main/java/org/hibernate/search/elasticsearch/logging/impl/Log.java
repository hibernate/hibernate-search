/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.ClassFormatter;
import org.hibernate.search.util.logging.impl.IndexedTypeIdentifierFormatter;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Hibernate Search log abstraction for the Elasticsearch integration.
 *
 * @author Gunnar Morling
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends org.hibernate.search.util.logging.impl.Log {

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 1,
			value = "Cannot execute query '%2$s', as targeted entity type '%1$s' is not mapped to an Elasticsearch index")
	SearchException cannotRunEsQueryTargetingEntityIndexedWithNonEsIndexManager(@FormatWith(IndexedTypeIdentifierFormatter.class) IndexedTypeIdentifier entityType, String query);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 2,
			value = "Lucene query '%1$s' cannot be transformed into equivalent Elasticsearch query" )
	SearchException cannotTransformLuceneQueryIntoEsQuery(Query query);

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
			value = "Elasticsearch request failed\nRequest:\n========\n%1$sResponse:\n=========\n%2$s"
	)
	SearchException elasticsearchRequestFailed(
			@FormatWith( ElasticsearchRequestFormatter.class ) ElasticsearchRequest request,
			@FormatWith( ElasticsearchResponseFormatter.class ) ElasticsearchResponse response,
			@Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 8,
			value = "Elasticsearch bulked request failed\nRequest:\n========\n%1$s\n%2$sResponse:\n=========\n%3$s"
	)
	SearchException elasticsearchBulkedRequestFailed(
			@FormatWith( ElasticsearchJsonObjectFormatter.class ) JsonObject requestMetadata,
			@FormatWith( ElasticsearchJsonObjectFormatter.class ) JsonObject requestBody,
			@FormatWith( ElasticsearchJsonObjectFormatter.class ) JsonObject response,
			@Cause Exception cause);

	@LogMessage(level = Level.WARN)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 9,
			value = "Field '%2$s' in '%1$s' requires an Elasticsearch analyzer reference (got '%3$s' instead). The analyzer will be ignored.")
	void analyzerIsNotElasticsearch(@FormatWith(IndexedTypeIdentifierFormatter.class) IndexedTypeIdentifier entityType, String fieldName, AnalyzerReference analyzerReference);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 10,
			value = "Elasticsearch connection time-out; check the cluster status, it should be 'green'" )
	SearchException elasticsearchRequestTimeout();

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
			value = "Unexpected numeric encoding type for field '%2$s' on entity type '%1$s'. "
					+ "If you used a custom field bridge, make sure it implements MetadataProvidingFieldBridge"
					+ " and provides metadata for this field."
	)
	SearchException unexpectedNumericEncodingType(@FormatWith(IndexedTypeIdentifierFormatter.class) IndexedTypeIdentifier entityType, String fieldName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 20,
			value = "Could not create mapping for entity type %1$s"
	)
	SearchException elasticsearchMappingCreationFailed(Object entityType, @Cause Exception cause);

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
			value = "Timed out while waiting for for index '%1$s' to reach status '%2$s'; status was still '%3$s' after %4$s."
	)
	SearchException unexpectedIndexStatus(String indexName, String expected, String actual, String timeoutAndUnit);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 25,
			value = "With an Elasticsearch backend it is not possible to get a ReaderProvider or an IndexReader"
	)
	UnsupportedOperationException indexManagerReaderProviderUnsupported();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 26,
			value = "Faceting request of type %1$s not supported"
	)
	SearchException facetingRequestHasUnsupportedType(String facetingRequestType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 27,
			value = "The 'indexNullAs' property for Boolean fields must represent a Boolean ('true' or 'false')." )
	IllegalArgumentException invalidNullMarkerForBoolean();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 28,
			value = "The 'indexNullAs' property for Calendar and Date fields must represent a date/time in ISO-8601"
					+ " format (yyyy-MM-dd'T'HH:mm:ssZ)." )
	IllegalArgumentException invalidNullMarkerForCalendarAndDate(@Cause Exception e);

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
	void unsupportedDynamicBoost(Class<?> boostStrategyType, @FormatWith(IndexedTypeIdentifierFormatter.class) IndexedTypeIdentifier entityType, String fieldPath);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 33,
			value = "An Elasticsearch schema validation failed: %1$s"
	)
	ElasticsearchSchemaValidationException schemaValidationFailed(String message);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 34,
			value = "Could not retrieve the mappings from Elasticsearch for validation"
	)
	SearchException elasticsearchMappingRetrievalForValidationFailed(@Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 35,
			value = "Could not update mappings in index '%1$s'"
	)
	SearchException schemaUpdateFailed(Object indexName, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 36,
			value = "Mapping conflict detected for field '%2$s' on entity '%1$s'."
					+ " The current mapping would require the field to be mapped to both a composite field"
					+ " ('object' datatype) and a \"concrete\" field ('integer', 'date', etc.) holding a value,"
					+ " which Elasticsearch does not allow. If you're seeing this issue, you probably added both"
					+ " an @IndexedEmbedded annotation and a @Field (or similar) annotation on the same property:"
					+ " if that's the case, please set either @IndexedEmbedded.prefix or @Field.name to a custom value"
					+ " different from the default to resolve the conflict."
	)
	SearchException fieldIsBothCompositeAndConcrete(@FormatWith(IndexedTypeIdentifierFormatter.class) IndexedTypeIdentifier entityType, String fieldPath);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 37,
			value = "The 'indexNullAs' property for Period fields must represent a date interval in ISO-8601"
					+ " format (for instance P3Y2M1D for 3 years, 2 months and 1 day)." )
	IllegalArgumentException invalidNullMarkerForPeriod(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 38,
			value = "The 'indexNullAs' property for Duration fields must represent a duration in ISO-8601"
					+ " format (for instance P1DT2H3M4.007S for 1 day, 2 hours, 3 minutes, 4 seconds and 7 miliseconds)." )
	IllegalArgumentException invalidNullMarkerForDuration(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 39,
			value = "The 'indexNullAs' property for Instant fields must represent a date/time in ISO-8601"
					+ " format (yyyy-MM-dd'T'HH:mm:ssZ[ZZZ])." )
	IllegalArgumentException invalidNullMarkerForInstant(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 40,
			value = "The 'indexNullAs' property for LocalDateTime fields must represent a local date/time in ISO-8601"
					+ " format (yyyy-MM-dd'T'HH:mm:ss)." )
	IllegalArgumentException invalidNullMarkerForLocalDateTime(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 41,
			value = "The 'indexNullAs' property for LocalDate fields must represent a local date in ISO-8601"
					+ " format (yyyy-MM-dd)." )
	IllegalArgumentException invalidNullMarkerForLocalDate(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 42,
			value = "The 'indexNullAs' property for LocalTime fields must represent a local time in ISO-8601"
					+ " format (HH:mm:ss)." )
	IllegalArgumentException invalidNullMarkerForLocalTime(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 43,
			value = "The 'indexNullAs' property for OffsetDateTime fields must represent an offset date/time in ISO-8601"
					+ " format (yyyy-MM-dd'T'HH:mm:ssZ)." )
	IllegalArgumentException invalidNullMarkerForOffsetDateTime(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 44,
			value = "The 'indexNullAs' property for OffsetTime fields must represent an offset time in ISO-8601"
					+ " format (HH:mm:ssZ)." )
	IllegalArgumentException invalidNullMarkerForOffsetTime(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 45,
			value = "The 'indexNullAs' property for ZonedDateTime fields must represent a zoned date/time in ISO-8601"
					+ " format (yyyy-MM-dd'T'HH:mm:ss[ZZZ])." )
	IllegalArgumentException invalidNullMarkerForZonedDateTime(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 46,
			value = "The 'indexNullAs' property for ZonedTime fields must represent a zoned time in ISO-8601"
					+ " format (HH:mm:ss[ZZZ])." )
	IllegalArgumentException invalidNullMarkerForZonedTime(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 47,
			value = "The 'indexNullAs' property for Year fields must represent a year in ISO-8601"
					+ " format (for instance 2014)." )
	IllegalArgumentException invalidNullMarkerForYear(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 48,
			value = "The 'indexNullAs' property for YearMonth fields must represent a year/month in ISO-8601"
					+ " format (yyyy-MM-dd)." )
	IllegalArgumentException invalidNullMarkerForYearMonth(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 49,
			value = "The 'indexNullAs' property for MonthDay fields must represent a month/day in ISO-8601"
					+ " format (--MM-dd)." )
	IllegalArgumentException invalidNullMarkerForMonthDay(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 50,
			value = "The index '%1$s' does not exist in the Elasticsearch cluster." )
	SearchException indexMissing(Object indexName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 51,
			value = "The given payload contains unsupported attributes: %1$s. Only 'query' is supported." )
	SearchException unsupportedSearchAPIPayloadAttributes(List<String> invalidAttributes);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 52,
			value = "The given payload is not a valid JSON object." )
	SearchException invalidSearchAPIPayload(@Cause Exception e);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 53,
			value = "Executing Elasticsearch query on '%s' with parameters '%s': <%s>" )
	void executingElasticsearchQuery(String path, Map<String, String> parameters,
			String bodyParts);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 54,
			value = "Invalid field path detected for field '%2$s' on entity '%1$s':"
					+ " the field name is not prefixed with '%3$s' as it should."
					+ " This probably means that the field was created with a custom field bridge which added"
					+ " fields with an arbitrary name, not taking the name passed as a parameter into account."
					+ " This is not supported with the Elasticsearch indexing service: please only add suffixes to the name"
					+ " passed as a parameter to the various bridge methods and never ignore this name."
	)
	SearchException indexedEmbeddedPrefixBypass(@FormatWith(IndexedTypeIdentifierFormatter.class) IndexedTypeIdentifier entityType, String fieldPath, String expectedParent);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 55,
			value = "The same tokenizer name '%1$s' is assigned to multiple definitions. The tokenizer names must be unique."
					+ " If you used the @TokenizerDef annotation and this name was automatically generated,"
					+ " you may override this name by using @TokenizerDef.name." )
	SearchException tokenizerNamingConflict(String remoteName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 56,
			value = "The same char filter name '%1$s' is assigned to multiple definitions. The char filter names must be unique."
					+ " If you used the @CharFilterDef annotation and this name was automatically generated,"
					+ " you may override this name by using @CharFilterDef.name." )
	SearchException charFilterNamingConflict(String remoteName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 57,
			value = "The same token filter name '%1$s' is assigned to multiple definitions. The token filter names must be unique."
					+ " If you used the @TokenFilterDef annotation and this name was automatically generated,"
					+ " you may override this name by using @TokenFilterDef.name." )
	SearchException tokenFilterNamingConflict(String remoteName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 58,
			value = "The char filter factory '%1$s' is not supported with Elasticsearch."
					+ " Please only use builtin Lucene factories that have a builtin equivalent in Elasticsearch." )
	SearchException unsupportedCharFilterFactory(@FormatWith(ClassFormatter.class) Class<?> factoryType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 59,
			value = "The tokenizer factory '%1$s' is not supported with Elasticsearch."
					+ " Please only use builtin Lucene factories that have a builtin equivalent in Elasticsearch." )
	SearchException unsupportedTokenizerFactory(@FormatWith(ClassFormatter.class) Class<?> factoryType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 60,
			value = "The token filter factory '%1$s' is not supported with Elasticsearch."
					+ " Please only use builtin Lucene factories that have a builtin equivalent in Elasticsearch." )
	SearchException unsupportedTokenFilterFactory(@FormatWith(ClassFormatter.class) Class<?> factoryType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 61,
			value = "The parameter '%2$s' is not supported for the factory '%1$s' with Elasticsearch." )
	SearchException unsupportedAnalysisFactoryParameter(@FormatWith(ClassFormatter.class) Class<?> factoryType, String parameter);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 62,
			value = "The parameter '%2$s' for the factory '%1$s' refers to the class '%3$s',"
					+ " which cannot be converted to a builtin Elasticsearch tokenizer type." )
	SearchException unsupportedAnalysisFactoryTokenizerClassNameParameter(@FormatWith(ClassFormatter.class) Class<?> factoryClass, String parameterName, String tokenizerClass);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 63,
			value = "The parameter '%2$s' for the factory '%1$s' has an unsupported value: '%3$s' is unsupported with Elasticsearch." )
	SearchException unsupportedAnalysisDefinitionParameterValue(@FormatWith(ClassFormatter.class) Class<?> factoryClass, String parameterName, String parameterValue);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 64,
			value = "The analyzer implementation '%1$s' is not supported with Elasticsearch."
					+ " Please only use builtin Lucene analyzers that have a builtin equivalent in Elasticsearch.")
	SearchException unsupportedAnalyzerImplementation(@FormatWith(ClassFormatter.class) Class<?> luceneClass);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 65,
			value = "The parameter '%2$s' for the factory '%1$s' could not be parsed as a JSON string: %3$s" )
	SearchException invalidAnalysisDefinitionJsonStringParameter(@FormatWith(ClassFormatter.class) Class<?> factoryClass, String parameterName, String causeMessage, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 66,
			value = "The parameter '%2$s' for the factory '%1$s' could not be parsed as JSON: %3$s" )
	SearchException invalidAnalysisDefinitionJsonParameter(@FormatWith(ClassFormatter.class) Class<?> factoryClass, String parameterName, String causeMessage, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 67,
			value = "Could not update settings for index '%1$s'"
	)
	SearchException elasticsearchSettingsUpdateFailed(Object indexName, @Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 68,
			value = "Could not retrieve the index settings from Elasticsearch for validation"
	)
	SearchException elasticsearchIndexSettingsRetrievalForValidationFailed(@Cause Exception cause);

	@LogMessage(level = Level.INFO)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 69,
			value = "Closed Elasticsearch index '%1$s' automatically."
	)
	void closedIndex(Object indexName);

	@LogMessage(level = Level.INFO)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 70,
			value = "Opened Elasticsearch index '%1$s' automatically."
	)
	void openedIndex(Object indexName);

	@LogMessage(level = Level.ERROR)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 71,
			value = "Failed to open Elasticsearch index '%1$s' ; see the stack trace below."
	)
	void failedToOpenIndex(Object indexName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 72,
			value = "DeleteByQuery request to Elasticsearch failed with 404 result code."
					+ "\nPlease check that 1. you installed the delete-by-query plugin on your Elasticsearch nodes"
					+ " and 2. the targeted index exists"
			)
	SearchException elasticsearch2RequestDeleteByQueryNotFound();

	@LogMessage(level = Level.WARN)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 73,
			value = "Hibernate Search will connect to Elasticsearch server '%1$s' with authentication over plain HTTP (not HTTPS)."
					+ " The password will be sent in clear text over the network."
			)
	void usingPasswordOverHttp(String serverUris);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 74,
			value = "The same analyzer name '%1$s' is assigned to multiple definitions. The analyzer names must be unique." )
	SearchException analyzerNamingConflict(String remoteName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 75,
			value = "Property '" + ElasticsearchEnvironment.ANALYSIS_DEFINITION_PROVIDER + "' set to value '%1$s' is invalid."
					+ " The value must be the fully-qualified name of a class with a public, no-arg constructor in your classpath."
					+ " Also, the class must either implement ElasticsearchAnalyzerDefinitionProvider or expose a public,"
					+ " @Factory-annotated method returning a ElasticsearchAnalyzerDefinitionProvider.")
	SearchException invalidElasticsearchAnalyzerDefinitionProvider(String providerClassName, @Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 76,
			value = "Invalid analyzer definition for name '%1$s'. Analyzer definitions must at least define the tokenizer." )
	SearchException invalidElasticsearchAnalyzerDefinition(String name);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 77,
			value = "Invalid tokenizer definition for name '%1$s'. Tokenizer definitions must at least define the tokenizer type." )
	SearchException invalidElasticsearchTokenizerDefinition(String name);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 78,
			value = "Invalid char filter definition for name '%1$s'. Char filter definitions must at least define the char filter type." )
	SearchException invalidElasticsearchCharFilterDefinition(String name);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 79,
			value = "Invalid token filter definition for name '%1$s'. Token filter definitions must at least define the token filter type." )
	SearchException invalidElasticsearchTokenFilterDefinition(String name);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 80,
			value = "Failed to detect the Elasticsearch version running on the cluster." )
	SearchException failedToDetectElasticsearchVersion(@Cause Exception e);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 81,
			value = "An unsupported Elasticsearch version runs on the Elasticsearch cluster: '%s'."
					+ " Please refer to the documentation to know which versions are supported." )
	SearchException unsupportedElasticsearchVersion(String name);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 82,
			value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s in %dms."
					+ " Response had status %d '%s'."
	)
	void executedRequest(String method, String path, Map<String, String> getParameters, long timeInMs,
			int responseStatusCode, String responseStatusMessage);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 83,
			value = "For simple query string queries, Elasticsearch does not support overriding fields with more than one different analyzers: %1$s.")
	SearchException unableToOverrideQueryAnalyzerWithMoreThanOneAnalyzersForSimpleQueryStringQueries(Collection<String> analyzers);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 84,
			value = "The parameter '%2$s' must have value '%3$s' for the factory '%1$s' with Elasticsearch. Current value '%4$s' is invalid." )
	SearchException invalidAnalysisFactoryParameter(@FormatWith(ClassFormatter.class) Class<?> factoryType, String parameter,
			String expectedValue, String actualValue);

	@LogMessage(level = Level.WARN)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 85,
			value = "Hibernate Search may not work correctly, because an unknown Elasticsearch version runs on the Elasticsearch cluster: '%s'." )
	void unexpectedElasticsearchVersion(String name);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 86,
			value = "The same normalizer name '%1$s' is assigned to multiple definitions. The analyzer names must be unique." )
	SearchException normalizerNamingConflict(String remoteName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 87,
			value = "The same name '%1$s' is assigned to a normalizer definition and an analyzer definition."
					+ " This is not possible on Elasticsearch 5.1 and below, since normalizers are translated to analyzers under the hood." )
	SearchException analyzerNormalizerNamingConflict(String remoteName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 88,
			value = "You cannot use @Normalizer(impl = \"%1$s\") on entities mapped to Elasticsearch:"
					+ " there are no built-in normalizers in Elasticsearch."
					+ " Use @Normalizer(definition = \"...\") instead." )
	SearchException cannotUseNormalizerImpl(@FormatWith(ClassFormatter.class) Class<?> analyzerType);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 89,
			value = "Failed to parse Elasticsearch response. Status code was '%1$d', status phrase was '%2$s'." )
	SearchException failedToParseElasticsearchResponse(int statusCode, String statusPhrase, @Cause Exception cause);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 90,
			value = "Elasticsearch response indicates a failure." )
	SearchException elasticsearchResponseIndicatesFailure();

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 91,
			value = "The thread was interrupted while a changeset was being submitted to '%1$s'."
					+ " The changeset has been discarded." )
	SearchException threadInterruptedWhileSubmittingChangeset(String orchestratorName);

	@Message(id = ES_BACKEND_MESSAGES_START_ID + 92,
			value = "A changeset was submitted after Hibernate Search shutdown was requested to '%1$s'."
					+ " The changeset has been discarded." )
	SearchException orchestratorShutDownBeforeSubmittingChangeset(String orchestratorName);

	@LogMessage(level = Level.TRACE)
	@Message(id = ES_BACKEND_MESSAGES_START_ID + 93,
		value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s in %dms."
				+ " Response had status %d '%s'. Request body: <%s>. Response body: <%s>"
	)
	void executedRequest(String method, String path, Map<String, String> getParameters, long timeInMs,
			int responseStatusCode, String responseStatusMessage,
			String requestBodyParts, String responseBody);
}
