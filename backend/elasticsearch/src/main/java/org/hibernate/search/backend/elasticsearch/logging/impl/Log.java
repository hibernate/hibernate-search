/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.jboss.logging.Logger.Level.WARN;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.logging.spi.AggregationKeyFormatter;
import org.hibernate.search.util.common.logging.impl.DurationInSecondsAndFractionsFormatter;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.BACKEND_ES_ID_RANGE_MIN, max = MessageConstants.BACKEND_ES_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5 (engine module)
		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5 (engine module).
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 (engine module) here

	// -----------------------------------
	// Pre-existing messages from Search 5 (ES module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.BACKEND_ES_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_2 + 7,
			value = "Elasticsearch request failed: %3$s\nRequest: %1$s\nResponse: %2$s"
	)
	SearchException elasticsearchRequestFailed(
			@FormatWith( ElasticsearchRequestFormatter.class ) ElasticsearchRequest request,
			@FormatWith( ElasticsearchResponseFormatter.class ) ElasticsearchResponse response,
			String causeMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 8,
			// Note: no need to add a '\n' before "Response", since the formatter will always add one
			value = "Elasticsearch bulked request failed: %3$s\nRequest metadata: %1$sResponse: %2$s"
	)
	SearchException elasticsearchBulkedRequestFailed(
			@FormatWith( ElasticsearchJsonObjectFormatter.class ) JsonObject requestMetadata,
			@FormatWith( ElasticsearchJsonObjectFormatter.class ) JsonObject response,
			String causeMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 10,
			value = "Elasticsearch connection time-out; check the cluster status, it should be 'green'" )
	SearchException elasticsearchRequestTimeout();

	@Message(id = ID_OFFSET_2 + 20,
			value = "Could not create mapping for index '%1$s': %2$s"
	)
	SearchException elasticsearchMappingCreationFailed(String indexName, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 22, value = "Invalid index status: '%1$s'."
			+ " Valid statuses are: %2$s.")
	SearchException invalidIndexStatus(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_2 + 24, value = "Index '%1$s' failed to reach status '%2$s' after %3$s.")
	SearchException unexpectedIndexStatus(URLEncodedString indexName, String expected, String timeoutAndUnit,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 34,
			value = "Could not retrieve the index metadata from Elasticsearch"
	)
	SearchException elasticsearchIndexMetadataRetrievalFailed(@Cause Throwable cause);

	@Message(id = ID_OFFSET_2 + 35,
			value = "Could not update mappings in index '%1$s': %2$s"
	)
	SearchException schemaUpdateFailed(URLEncodedString indexName, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 50,
			value = "Index aliases [%1$s, %2$s] do not point to any index in the Elasticsearch cluster." )
	SearchException indexMissing(URLEncodedString write, URLEncodedString read);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET_2 + 53,
			value = "Executing Elasticsearch query on '%s' with parameters '%s': <%s>" )
	void executingElasticsearchQuery(String path, Map<String, String> parameters,
			String bodyParts);

	@Message(id = ID_OFFSET_2 + 55,
			value = "Multiple tokenizer definitions with the same name: '%1$s'. The tokenizer names must be unique.")
	SearchException tokenizerNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_2 + 56,
			value = "Multiple char filter definitions with the same name: '%1$s'. The char filter names must be unique.")
	SearchException charFilterNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_2 + 57,
			value = "Multiple token filter definitions with the same name: '%1$s'. The token filter names must be unique.")
	SearchException tokenFilterNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_2 + 67,
			value = "Could not update settings for index '%1$s'"
	)
	SearchException elasticsearchSettingsUpdateFailed(Object indexName, @Cause Exception e);

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET_2 + 69,
			value = "Closed Elasticsearch index '%1$s' automatically."
	)
	void closedIndex(Object indexName);

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET_2 + 70,
			value = "Opened Elasticsearch index '%1$s' automatically."
	)
	void openedIndex(Object indexName);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_2 + 73,
			value = "Hibernate Search will connect to Elasticsearch with authentication over plain HTTP (not HTTPS)."
					+ " The password will be sent in clear text over the network."
	)
	void usingPasswordOverHttp();

	@Message(id = ID_OFFSET_2 + 74,
			value = "Multiple analyzer definitions with the same name: '%1$s'. The analyzer names must be unique.")
	SearchException analyzerNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_2 + 75,
			value = "Error while applying analysis configuration: %1$s")
	SearchException unableToApplyAnalysisConfiguration(String errorMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 76,
			value = "Invalid analyzer definition for name '%1$s'. Analyzer definitions must at least define the tokenizer.")
	SearchException invalidElasticsearchAnalyzerDefinition(String name);

	@Message(id = ID_OFFSET_2 + 77,
			value = "Invalid tokenizer definition for name '%1$s'. Tokenizer definitions must at least define the tokenizer type.")
	SearchException invalidElasticsearchTokenizerDefinition(String name);

	@Message(id = ID_OFFSET_2 + 78,
			value = "Invalid char filter definition for name '%1$s'. Char filter definitions must at least define the char filter type.")
	SearchException invalidElasticsearchCharFilterDefinition(String name);

	@Message(id = ID_OFFSET_2 + 79,
			value = "Invalid token filter definition for name '%1$s'. Token filter definitions must at least define the token filter type.")
	SearchException invalidElasticsearchTokenFilterDefinition(String name);

	@Message(id = ID_OFFSET_2 + 80,
			value = "Failed to detect the Elasticsearch version running on the cluster: %s" )
	SearchException failedToDetectElasticsearchVersion(String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 81,
			value = "An unsupported Elasticsearch version runs on the Elasticsearch cluster: '%s'."
					+ " Please refer to the documentation to know which versions are supported." )
	SearchException unsupportedElasticsearchVersion(ElasticsearchVersion version);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET_2 + 82,
			value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s and %d objects in payload in %dms."
					+ " Response had status %d '%s'."
	)
	void executedRequest(String method, String path, Map<String, String> getParameters, int bodyParts, long timeInMs,
			int responseStatusCode, String responseStatusMessage);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_2 + 85,
			value = "Hibernate Search may not work correctly, because an unknown Elasticsearch version runs on the Elasticsearch cluster: '%s'." )
	void unknownElasticsearchVersion(ElasticsearchVersion version);

	@Message(id = ID_OFFSET_2 + 86,
			value = "Multiple normalizer definitions with the same name: '%1$s'. The normalizer names must be unique.")
	SearchException normalizerNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_2 + 89,
			value = "Failed to parse Elasticsearch response. Status code was '%1$d', status phrase was '%2$s'.")
	SearchException failedToParseElasticsearchResponse(int statusCode, String statusPhrase, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 90,
			value = "Elasticsearch response indicates a failure." )
	SearchException elasticsearchResponseIndicatesFailure();

	@LogMessage(level = Level.TRACE)
	@Message(id = ID_OFFSET_2 + 93,
			value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s and %d objects in payload in %dms."
					+ " Response had status %d '%s'. Request body: <%s>. Response body: <%s>"
	)
	void executedRequest(String method, String path, Map<String, String> getParameters, int bodyParts, long timeInMs,
			int responseStatusCode, String responseStatusMessage,
			String requestBodyParts, String responseBody);

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 (ES module) here

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_3 = MessageConstants.BACKEND_ES_ID_RANGE_MIN + 500;

	@Message(id = ID_OFFSET_3 + 2,
			value = "A multi-index scope cannot include both an Elasticsearch index and another type of index."
					+ " Base scope was: '%1$s', Elasticsearch index was: '%2$s'")
	SearchException cannotMixElasticsearchScopeWithOtherType(IndexScopeBuilder baseScope,
			ElasticsearchIndexManager elasticsearchIndex, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 3,
			value = "A multi-index scope cannot span multiple Elasticsearch backends."
					+ " Base scope was: '%1$s', index from another backend was: '%2$s'")
	SearchException cannotMixElasticsearchScopeWithOtherBackend(IndexScopeBuilder baseScope,
			ElasticsearchIndexManager indexFromOtherBackend, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 4,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForSearch(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 6,
			value = "The Elasticsearch extension can only be applied to objects"
			+ " derived from the Elasticsearch backend. Was applied to '%1$s' instead.")
	SearchException elasticsearchExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET_3 + 8,
			value = "An Elasticsearch query cannot include search predicates built using a non-Elasticsearch search scope."
					+ " Given predicate was: '%1$s'")
	SearchException cannotMixElasticsearchSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = ID_OFFSET_3 + 9,
			value = "Field '%1$s' is not an object field.")
	SearchException nonObjectFieldForNestedQuery(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 10,
			value = "Object field '%1$s' is not stored as nested.")
	SearchException nonNestedFieldForNestedQuery(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 11,
			value = "An Elasticsearch query cannot include search sorts built using a non-Elasticsearch search scope."
					+ " Given sort was: '%1$s'")
	SearchException cannotMixElasticsearchSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = ID_OFFSET_3 + 14,
			value = "Index '%1$s' requires multi-tenancy but the backend does not support it in its current configuration.")
	SearchException multiTenancyRequiredButNotSupportedByBackend(String indexName, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 15, value = "Invalid multi-tenancy strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_3 + 16,
			value = "Tenant identifier '%1$s' is provided, but multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(String tenantId, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 17,
			value = "Backend has multi-tenancy enabled, but no tenant identifier is provided.")
	SearchException multiTenancyEnabledButNoTenantIdProvided(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 18,
			value = "Attempt to unwrap the Elasticsearch low-level client to %1$s,"
					+ " but the client can only be unwrapped to %2$s.")
	SearchException clientUnwrappingWithUnkownType(Class<?> requestedClass, Class<?> actualClass);

	@Message(id = ID_OFFSET_3 + 19,
			value = "Attempt to unwrap an Elasticsearch backend to '%1$s',"
					+ " but this backend can only be unwrapped to '%2$s'.")
	SearchException backendUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 20,
			value = "The index schema node '%1$s' was added twice."
					+ " Multiple bridges may be trying to access the same index field, "
					+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.")
	SearchException indexSchemaNodeNameConflict(String name,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 23,
			value = "Range lookups (range predicates, range aggregations) are not supported by this field's type (GeoPoint). Use spatial features instead.")
	SearchException rangesNotSupportedByGeoPoint(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 24,
			value = "Direct value lookups (match predicates, terms aggregations) are not supported by this field's type (GeoPoint). Use spatial features instead.")
	SearchException directValueLookupNotSupportedByGeoPoint(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 25,
			value = "Invalid field reference for this document element: this document element has path '%1$s', but the referenced field has a parent with path '%2$s'.")
	SearchException invalidFieldForDocumentElement(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET_3 + 26,
			value = "Expected data was missing in the Elasticsearch response.")
	AssertionFailure elasticsearchResponseMissingData();

	@Message(id = ID_OFFSET_3 + 27,
			value = "Spatial predicates are not supported by this field's type.")
	SearchException spatialPredicatesNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 28,
			value = "Distance related operations are not supported by this field's type.")
	SearchException distanceOperationsNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 29,
			value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ceiling);

	@Message(id = ID_OFFSET_3 + 30,
			value = "Conflicting index names: Hibernate Search indexes '%1$s' and '%2$s' both target the name or alias '%3$s'")
	SearchException conflictingIndexNames(String firstHibernateSearchIndexName,
			String secondHibernateSearchIndexName, String nameOrAlias);

	@Message(id = ID_OFFSET_3 + 31,
			value = "Could not resolve index name '%1$s' to an entity type: %2$s")
	SearchException elasticsearchResponseUnknownIndexName(String elasticsearchIndexName, String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_3 + 32,
			value = "Unable to convert DSL parameter: %1$s")
	SearchException cannotConvertDslParameter(String errorMessage, @Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 33,
			value = "Attempt to unwrap an Elasticsearch index manager to '%1$s',"
					+ " but this index manager can only be unwrapped to '%2$s'.")
	SearchException indexManagerUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 34,
			value = "Invalid typed analyzer definition for name '%1$s'. Typed analyzer definitions must at least define the analyzer type.")
	SearchException invalidElasticsearchTypedAnalyzerDefinition(String name);

	@Message(id = ID_OFFSET_3 + 35,
			value = "Cannot apply both an analyzer and a normalizer. Analyzer: '%1$s', normalizer: '%2$s'.")
	SearchException cannotApplyAnalyzerAndNormalizer(String analyzerName, String normalizerName, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 36,
			value = "Cannot apply an analyzer on a sortable field. Use a normalizer instead. Analyzer: '%1$s'."
					+ " If an actual analyzer (with tokenization) is necessary, define two separate fields:"
					+ " one with an analyzer that is not sortable, and one with a normalizer that is sortable.")
	SearchException cannotUseAnalyzerOnSortableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 37,
			value = "Multiple parameters with the same name: '%1$s'. Can't assign both value '%2$s' and '%3$s'" )
	SearchException analysisComponentParameterConflict(String name, JsonElement value1, JsonElement value2);

	@Message(id = ID_OFFSET_3 + 38,
			value = "An Elasticsearch query cannot include search projections built using a non-Elasticsearch search scope."
			+ " Given projection was: '%1$s'")
	SearchException cannotMixElasticsearchSearchQueryWithOtherProjections(SearchProjection<?> projection);

	@Message(id = ID_OFFSET_3 + 39, value = "Invalid type '%2$s' for projection on field '%1$s'.")
	SearchException invalidProjectionInvalidType(String absoluteFieldPath,
			@FormatWith(ClassFormatter.class) Class<?> type,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 40,
			value = "Traditional sorting operations are not supported by the GeoPoint field type, use distance sorting instead.")
	SearchException traditionalSortNotSupportedByGeoPoint(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 41,
			value = "Multiple conflicting types to build a predicate for field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingFieldTypesForPredicate(String absoluteFieldPath,
			ElasticsearchFieldPredicateBuilderFactory component1, ElasticsearchFieldPredicateBuilderFactory component2,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 42,
			value = "Multiple conflicting types to build a sort for field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingFieldTypesForSort(String absoluteFieldPath,
			ElasticsearchFieldSortBuilderFactory component1, ElasticsearchFieldSortBuilderFactory component2,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 43,
			value = "Multiple conflicting types to build a projection for field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingFieldTypesForProjection(String absoluteFieldPath,
			ElasticsearchFieldProjectionBuilderFactory component1, ElasticsearchFieldProjectionBuilderFactory component2,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 44, value = "Failed to shut down the Elasticsearch backend.")
	SearchException failedToShutdownBackend(@Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 45, value = "Cannot guess field type for input type %1$s.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 47,
			value = "Projections are not enabled for field '%1$s'. Make sure the field is marked as projectable.")
	SearchException nonProjectableField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 48,
			value = "Sorting is not enabled for field '%1$s'. Make sure the field is marked as sortable.")
	SearchException unsortableField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 49,
			value = "Multiple conflicting types for identifier: '%1$s' vs. '%2$s'.")
	SearchException conflictingIdentifierTypesForPredicate(ToDocumentIdentifierValueConverter<?> component1,
			ToDocumentIdentifierValueConverter<?> component2, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 50, value = "Failed to shut down the Elasticsearch index manager with name '%1$s'.")
	SearchException failedToShutdownIndexManager(String indexName, @Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 51, value = "The operation was skipped due to the failure of a previous work in the same workset.")
	SearchException elasticsearchSkippedBecauseOfPreviousWork(@Cause Throwable skippingCause);

	@Message(id = ID_OFFSET_3 + 52, value = "Invalid index lifecycle strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidIndexLifecycleStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_3 + 53,
			value = "Text predicates (phrase, fuzzy, wildcard, simple query string) are not supported by this field's type.")
	SearchException textPredicatesNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 54,
			value = "Incomplete field definition."
					+ " You must call toReference() to complete the field definition.")
	SearchException incompleteFieldDefinition(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 55,
			value = "Multiple calls to toReference() for the same field definition."
					+ " You must call toReference() exactly once.")
	SearchException cannotCreateReferenceMultipleTimes(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 56, value = "Invalid Elasticsearch version: '%1$s'."
			+ " The version must be in the form 'x.y.z-qualifier', where 'x', 'y' and 'z' are integers,"
			+ " and 'qualifier' is an string of word characters (alphanumeric or '_')."
			+ " Incomplete versions are allowed, for example '7.0' or just '7'.")
	SearchException invalidElasticsearchVersion(String versionString);

	@Message(id = ID_OFFSET_3 + 59, value = "Unexpected Elasticsearch version running on the cluster: '%2$s'."
			+ " Hibernate Search was configured for Elasticsearch '%1$s'.")
	SearchException unexpectedElasticsearchVersion(ElasticsearchVersion configuredVersion,
			ElasticsearchVersion actualVersion);

	@Message(id = ID_OFFSET_3 + 60, value = "Elasticsearch backend does not support skip analysis on not analyzed field: '%1$s'.")
	SearchException skipAnalysisOnKeywordField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 61,
			value = "Ambiguous Elasticsearch version: '%s'."
					+ " This version matches multiple dialects."
					+ " Please use a more precise version to remove the ambiguity." )
	SearchException ambiguousElasticsearchVersion(ElasticsearchVersion version);

	@Message(id = ID_OFFSET_3 + 62, value = "Index-null-as option is not supported on analyzed field. Trying to define the analyzer: '%1$s' together with index null as: '%2$s'.")
	SearchException cannotUseIndexNullAsAndAnalyzer(String analyzerName, String indexNullAs, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 63,
			value = "Multiple values were added to single-valued field '%1$s'."
					+ " Declare the field as multi-valued in order to allow this."
	)
	SearchException multipleValuesForSingleValuedField(String absolutePath);

	@Message(id = ID_OFFSET_3 + 64,
			value = "explain(String id) cannot be used when the query targets multiple indexes."
					+ " Use explain(String indexName, String id) and pass one of %1$s as the index name." )
	SearchException explainRequiresIndexName(Set<String> targetedIndexNames);

	@Message(id = ID_OFFSET_3 + 65,
			value = "The given index name '%2$s' is not among the indexes targeted by this query: %1$s." )
	SearchException explainRequiresIndexTargetedByQuery(Set<String> targetedIndexNames, String indexName);

	@Message(id = ID_OFFSET_3 + 66,
			value = "Document with id '%1$s' does not exist in the targeted index and thus its match cannot be explained." )
	SearchException explainUnknownDocument(URLEncodedString id);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_3 + 67,
			value = "'%s' was interrupted while waiting for index activity to finish. Index might be inconsistent or have a stale lock")
	void interruptedWhileWaitingForIndexActivity(String name, @Cause InterruptedException e);

	@Message(id = ID_OFFSET_3 + 68, value = "Impossible to detect a decimal scale to use for this field."
			+ " If the value is bridged, set '.asBigDecimal().decimalScale( int )' in the bind, else verify your mapping.")
	SearchException nullDecimalScale(@Param EventContext eventContext);

	@Message(id = ID_OFFSET_3 + 69, value = "The value '%1$s' cannot be indexed because its absolute value is too large.")
	SearchException scaledNumberTooLarge(Number value);

	@Message(id = ID_OFFSET_3 + 70, value = "Positive decimal scale ['%1$s'] is not allowed for BigInteger fields, since a BigInteger value cannot have any decimal digits.")
	SearchException invalidDecimalScale(Integer decimalScale, @Param EventContext eventContext);

	@Message(id = ID_OFFSET_3 + 71, value = "Field '%1$s' is not searchable. Make sure the field is marked as searchable.")
	SearchException nonSearchableField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 72, value = "The predicate '%1$s' is defined on a scope targeting different indexes."
			+ " Predicate is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException predicateDefinedOnDifferentIndexes(SearchPredicate predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_3 + 73, value = "The sort '%1$s' is defined on a scope targeting different indexes."
			+ " Sort is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException sortDefinedOnDifferentIndexes(SearchSort predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_3 + 74, value = "The projection '%1$s' is defined on a scope targeting different indexes."
			+ " Projection is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException projectionDefinedOnDifferentIndexes(SearchProjection<?> predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_3 + 75, value = "Multiple conflicting nested document paths to build a projection for field '%1$s'. '%2$s' vs. '%3$s'.")
	SearchException conflictingNestedDocumentPathsForProjection(String absoluteFieldPath, String nestedDocumentPath1, String nestedDocumentPath2, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 76,
			value = "Cannot apply an analyzer on an aggregable field. Use a normalizer instead. Analyzer: '%1$s'."
					+ " If an actual analyzer (with tokenization) is necessary, define two separate fields:"
					+ " one with an analyzer that is not aggregable, and one with a normalizer that is aggregable.")
	SearchException cannotUseAnalyzerOnAggregableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 77,
			value = "Aggregations are not enabled for field '%1$s'. Make sure the field is marked as aggregable.")
	SearchException nonAggregableField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 78, value = "Invalid type '%2$s' for aggregation on field '%1$s'.")
	SearchException invalidAggregationInvalidType(String absoluteFieldPath,
			@FormatWith(ClassFormatter.class) Class<?> type,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 79,
			value = "Multiple conflicting types to build an aggregation for field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingFieldTypesForAggregation(String absoluteFieldPath,
			ElasticsearchFieldAggregationBuilderFactory component1,
			ElasticsearchFieldAggregationBuilderFactory component2,
			@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 80,
			value = "Elasticsearch range aggregations only accept ranges in the canonical form:"
					+ " (-Infinity, <value>) or [<value1>, <value2>) or [<value>, +Infinity)."
					+ " The given range is not in canonical form: '%1$s'.")
	SearchException elasticsearchRangeAggregationRequiresCanonicalFormForRanges(Range<?> range);

	@Message(id = ID_OFFSET_3 + 81,
			value = "An Elasticsearch query cannot include search aggregations built using a non-Elasticsearch search scope."
					+ " Given aggregation was: '%1$s'")
	SearchException cannotMixElasticsearchSearchQueryWithOtherAggregations(SearchAggregation<?> aggregation);

	@Message(id = ID_OFFSET_3 + 82, value = "The aggregation '%1$s' is defined on a scope targeting different indexes."
			+ " Aggregation is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException aggregationDefinedOnDifferentIndexes(SearchAggregation<?> aggregation,
			Set<String> aggregationIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_3 + 83,
			value = "Terms aggregations are not supported by this field's type (string field with analyzed). Use a normalized field instead.")
	SearchException termsAggregationsNotSupportedByAnalyzedTextFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 84,
			value = "Range aggregations are not supported by this field's type.")
	SearchException rangeAggregationsNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET_3 + 85,
			value = "Multiple aggregations with the same key: '%1$s'")
	SearchException duplicateAggregationKey(@FormatWith(AggregationKeyFormatter.class) AggregationKey key);

	@Message(id = ID_OFFSET_3 + 86, value = "Multiple conflicting nested document paths to build a projection for field '%1$s'. '%2$s' vs. '%3$s'.")
	SearchException conflictingNestedDocumentPathHierarchyForProjection(String absoluteFieldPath,
			List<String> nestedDocumentPathHierarchy1, List<String> nestedDocumentPathHierarchy2, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 87, value = "Cannot apply a search analyzer if an analyzer has not been defined on the same field." +
			" Search analyzer: '%1$s'.")
	SearchException searchAnalyzerWithoutAnalyzer(String searchAnalyzer, @Param EventContext context);

	@Message(id = ID_OFFSET_3 + 88, value = "The operation failed due to the failure of the call to the bulk REST API.")
	SearchException elasticsearchFailedBecauseOfBulkFailure(@Cause Throwable bulkFailure);

	@Message(id = ID_OFFSET_3 + 89, value = "Invalid host/port: '%1$s'."
			+ " The host/port string must use the format 'host:port', for example 'mycompany.com:9200'"
			+ " The URI scheme ('http://', 'https://') must not be included.")
	SearchException invalidHostAndPort(String hostAndPort, @Cause Exception e);

	@Message(id = ID_OFFSET_3 + 90, value = "Request exceeded the timeout of %1$s: '%2$s'.")
	SearchTimeoutException timedOut(@FormatWith(DurationInSecondsAndFractionsFormatter.class) Duration timeout,
			@FormatWith(ElasticsearchRequestFormatter.class) ElasticsearchRequest request);

	@Message(id = ID_OFFSET_3 + 91, value = "Invalid name for the type-name mapping strategy: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidTypeNameMappingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_3 + 92,
			value = "Missing field '%1$s' for one of the search hits."
					+ " The document was probably indexed with a different configuration: full reindexing is necessary.")
	SearchException missingTypeFieldInDocument(String fieldName);

	@Message(id = ID_OFFSET_3 + 93,
			value = "Index aliases [%1$s, %2$s] are assigned to a single Hibernate Search index, "
					+ " but they are already defined in Elasticsearch and point to multiple distinct indexes: %3$s.")
	SearchException elasticsearchIndexNameAndAliasesMatchMultipleIndexes(URLEncodedString write, URLEncodedString read,
			Set<String> matchingIndexes);

	@Message(id = ID_OFFSET_3 + 94,
			value = "Index primary name  '%1$s' does not match the expected pattern '%2$s'.")
	SearchException invalidIndexPrimaryName(String elasticsearchIndexName, Pattern pattern);

	@Message(id = ID_OFFSET_3 + 95,
			value = "Unique key '%1$s' extracted from the index name does not match any of %2$s")
	SearchException invalidIndexUniqueKey(String uniqueKey, Set<String> knownKeys);

	@Message(id = ID_OFFSET_3 + 96,
			value = "Write alias and read alias must be different, but were set to the same value: '%1$s'.")
	SearchException sameWriteAndReadAliases(URLEncodedString writeAndReadAlias, @Param EventContext eventContext);

	@Message(id = ID_OFFSET_3 + 97, value = "Invalid Elasticsearch version: '%1$s'."
			+ " When version_check.enabled is set to false, "
			+ " the version must at least be in the form 'x.y', where 'x' and 'y' are integers")
	SearchException invalidElasticsearchVersionCheckConfiguration(String versionString);

	@Message(id = ID_OFFSET_3 + 98, value = "The lifecycle strategy cannot be set at the index level anymore."
			+ " Set the schema management strategy via the property 'hibernate.search.schema_management.strategy' instead.")
	SearchException lifecycleStrategyMovedToMapper();

	@Message(id = ID_OFFSET_3 + 99, value = "Simple query string targets fields [%1$s, %3$s] spanning multiple nested paths: %2$s, %4$s.")
	SearchException simpleQueryStringSpanningMultipleNestedPaths(String fieldPath1, String nestedPath1, String fieldPath2, String nestedPath2);
}
