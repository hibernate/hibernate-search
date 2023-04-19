/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.logging.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.logging.spi.AggregationKeyFormatter;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.DurationInSecondsAndFractionsFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
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
import org.apache.http.HttpHost;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.BACKEND_ES_ID_RANGE_MIN, max = MessageConstants.BACKEND_ES_ID_RANGE_MAX),
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (ES module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ES = MessageConstants.BACKEND_ES_ID_RANGE_MIN;

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
			value = "Elasticsearch response indicates a timeout (HTTP status 408)" )
	SearchException elasticsearchStatus408RequestTimeout();

	@Message(id = ID_OFFSET_LEGACY_ES + 20,
			value = "Unable to update mapping for index '%1$s': %2$s")
	SearchException elasticsearchMappingUpdateFailed(String indexName, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 22, value = "Invalid index status: '%1$s'."
			+ " Valid statuses are: %2$s.")
	SearchException invalidIndexStatus(String invalidRepresentation, List<String> validRepresentations);

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
			value = "Missing index: index names [%1$s, %2$s] do not point to any index in the Elasticsearch cluster." )
	SearchException indexMissing(URLEncodedString write, URLEncodedString read);

	@LogMessage(level = Level.TRACE)
	@Message(id = ID_OFFSET_LEGACY_ES + 53,
			value = "Executing Elasticsearch query on '%s' with parameters '%s': <%s>" )
	void executingElasticsearchQuery(String path, Map<String, String> parameters,
			String bodyParts);

	@Message(id = ID_OFFSET_LEGACY_ES + 55,
			value = "Duplicate tokenizer definitions: '%1$s'. Tokenizer names must be unique.")
	SearchException tokenizerNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_LEGACY_ES + 56,
			value = "Duplicate char filter definitions: '%1$s'. Char filter names must be unique.")
	SearchException charFilterNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_LEGACY_ES + 57,
			value = "Duplicate token filter definitions: '%1$s'. Token filter names must be unique.")
	SearchException tokenFilterNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_LEGACY_ES + 67,
			value = "Unable to update settings for index '%1$s': %2$s")
	SearchException elasticsearchSettingsUpdateFailed(Object indexName, String causeMessage, @Cause Exception cause);

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET_LEGACY_ES + 69,
			value = "Closed Elasticsearch index '%1$s' automatically.")
	void closedIndex(Object indexName);

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET_LEGACY_ES + 70,
			value = "Opened Elasticsearch index '%1$s' automatically.")
	void openedIndex(Object indexName);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ES + 73,
			value = "Hibernate Search will connect to Elasticsearch with authentication over plain HTTP (not HTTPS)."
					+ " The password will be sent in clear text over the network.")
	void usingPasswordOverHttp();

	@Message(id = ID_OFFSET_LEGACY_ES + 75,
			value = "Unable to apply analysis configuration: %1$s")
	SearchException unableToApplyAnalysisConfiguration(String errorMessage, @Cause Exception e,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET_LEGACY_ES + 76,
			value = "Invalid analyzer definition for name '%1$s'. Analyzer definitions must at least define the tokenizer.")
	SearchException invalidElasticsearchAnalyzerDefinition(String name);

	@Message(id = ID_OFFSET_LEGACY_ES + 77,
			value = "Invalid tokenizer definition for name '%1$s'. Tokenizer definitions must at least define the tokenizer type.")
	SearchException invalidElasticsearchTokenizerDefinition(String name);

	@Message(id = ID_OFFSET_LEGACY_ES + 78,
			value = "Invalid char filter definition for name '%1$s'. Char filter definitions must at least define the char filter type.")
	SearchException invalidElasticsearchCharFilterDefinition(String name);

	@Message(id = ID_OFFSET_LEGACY_ES + 79,
			value = "Invalid token filter definition for name '%1$s'. Token filter definitions must at least define the token filter type.")
	SearchException invalidElasticsearchTokenFilterDefinition(String name);

	@Message(id = ID_OFFSET_LEGACY_ES + 80,
			value = "Unable to detect the Elasticsearch version running on the cluster: %s")
	SearchException failedToDetectElasticsearchVersion(String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ES + 81,
			value = "Incompatible Elasticsearch version running on the cluster: '%s'."
					+ " Refer to the documentation to know which versions of Elasticsearch"
					+ " are compatible with Hibernate Search.")
	SearchException unsupportedElasticsearchVersion(ElasticsearchVersion version);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET_LEGACY_ES + 82,
			value = "Executed Elasticsearch HTTP %s request to '%s' with path '%s',"
					+ " query parameters %s and %d objects in payload in %dms."
					+ " Response had status %d '%s'. Request body: <%s>. Response body: <%s>")
	void executedRequestWithFailure(String method, HttpHost host, String path, Map<String, String> getParameters,
			int bodyParts, long timeInMs,
			int responseStatusCode, String responseStatusMessage,
			String requestBodyParts, String responseBody);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ES + 85,
			value = "Unknown Elasticsearch version running on the cluster: '%s'."
					+ " Hibernate Search may not work correctly."
					+ " Consider updating to a newer version of Hibernate Search, if any.")
	void unknownElasticsearchVersion(ElasticsearchVersion version);

	@Message(id = ID_OFFSET_LEGACY_ES + 89,
			value = "Unable to parse Elasticsearch response. Status code was '%1$d', status phrase was '%2$s'."
					+ " Nested exception: %3$s")
	SearchException failedToParseElasticsearchResponse(int statusCode, String statusPhrase,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 90,
			value = "Elasticsearch response indicates a failure.")
	SearchException elasticsearchResponseIndicatesFailure();

	@LogMessage(level = Level.TRACE)
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
	int ID_OFFSET = MessageConstants.BACKEND_ES_ID_RANGE_MIN + 500;

	@Message(id = ID_OFFSET + 2,
			value = "Invalid multi-index scope: a scope cannot span both a Elasticsearch index and another type of index."
					+ " Base scope: '%1$s', incompatible (Elasticsearch) index: '%2$s'.")
	SearchException cannotMixElasticsearchScopeWithOtherType(IndexScopeBuilder baseScope,
			ElasticsearchIndexManager elasticsearchIndex, @Param EventContext context);

	@Message(id = ID_OFFSET + 3,
			value = "Invalid multi-index scope: a scope cannot span multiple Elasticsearch backends."
					+ " Base scope: '%1$s', incompatible index (from another backend): '%2$s'.")
	SearchException cannotMixElasticsearchScopeWithOtherBackend(IndexScopeBuilder baseScope,
			ElasticsearchIndexManager indexFromOtherBackend, @Param EventContext context);

	@Message(id = ID_OFFSET + 6,
			value = "Invalid target for Elasticsearch extension: '%1$s'."
					+ " This extension can only be applied to components created by an Elasticsearch backend.")
	SearchException elasticsearchExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET + 8,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = ID_OFFSET + 11,
			value = "Invalid search sort: '%1$s'. You must build the sort from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = ID_OFFSET + 15, value = "Invalid multi-tenancy strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 16,
			value = "Invalid tenant identifiers: '%1$s'."
					+ " No tenant identifier is expected, because multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(Set<String> tenantIds, @Param EventContext context);

	@Message(id = ID_OFFSET + 17,
			value = "Missing tenant identifier."
					+ " A tenant identifier is expected, because multi-tenancy is enabled for this backend.")
	SearchException multiTenancyEnabledButNoTenantIdProvided(@Param EventContext context);

	@Message(id = ID_OFFSET + 18,
			value = "Invalid requested type for client: '%1$s'."
					+ " The Elasticsearch low-level client can only be unwrapped to '%2$s'.")
	SearchException clientUnwrappingWithUnkownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass);

	@Message(id = ID_OFFSET + 19,
			value = "Invalid requested type for this backend: '%1$s'."
					+ " Elasticsearch backends can only be unwrapped to '%2$s'.")
	SearchException backendUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 20,
			value = "Duplicate index field definition: '%1$s'."
					+ " Index field names must be unique."
					+ " Look for two property mappings with the same field name,"
					+ " or two indexed-embeddeds with prefixes that lead to conflicting index field names,"
					+ " or two custom bridges declaring index fields with the same name.")
	SearchException indexSchemaNodeNameConflict(String name,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 25,
			value = "Invalid field reference for this document element:"
					+ " this document element has path '%1$s', but the referenced field has a parent with path '%2$s'.")
	SearchException invalidFieldForDocumentElement(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET + 26,
			value = "Missing data in the Elasticsearch response.")
	AssertionFailure elasticsearchResponseMissingData();

	@Message(id = ID_OFFSET + 29,
			value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ceiling);

	@Message(id = ID_OFFSET + 30,
			value = "Conflicting index names: Hibernate Search indexes '%1$s' and '%2$s'"
					+ " both target the Elasticsearch index name or alias '%3$s'")
	SearchException conflictingIndexNames(String firstHibernateSearchIndexName,
			String secondHibernateSearchIndexName, String nameOrAlias);

	@Message(id = ID_OFFSET + 31,
			value = "Unable to resolve index name '%1$s' to an entity type: %2$s")
	SearchException elasticsearchResponseUnknownIndexName(String elasticsearchIndexName, String causeMessage,
			@Cause Exception e);

	@Message(id = ID_OFFSET + 32,
			value = "Unable to convert DSL argument: %1$s")
	SearchException cannotConvertDslParameter(String errorMessage, @Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET + 33,
			value = "Invalid requested type for this index manager: '%1$s'."
					+ " Elasticsearch index managers can only be unwrapped to '%2$s'.")
	SearchException indexManagerUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 34,
			value = "Invalid typed analyzer definition for name '%1$s'. Typed analyzer definitions must at least define the analyzer type.")
	SearchException invalidElasticsearchTypedAnalyzerDefinition(String name);

	@Message(id = ID_OFFSET + 35,
			value = "Invalid index field type: both analyzer '%1$s' and normalizer '%2$s' are assigned to this type."
					+ " Either an analyzer or a normalizer can be assigned, but not both.")
	SearchException cannotApplyAnalyzerAndNormalizer(String analyzerName, String normalizerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 36,
			value = "Invalid index field type: both analyzer '%1$s' and sorts are enabled."
					+ " Sorts are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not sortable, and one with a normalizer that is sortable.")
	SearchException cannotUseAnalyzerOnSortableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 37,
			value = "Ambiguous value for parameter '%1$s': this parameter is set to two different values '%2$s' and '%3$s'.")
	SearchException analysisComponentParameterConflict(String name, JsonElement value1, JsonElement value2);

	@Message(id = ID_OFFSET + 38,
			value = "Invalid search projection: '%1$s'. You must build the projection from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherProjections(SearchProjection<?> projection);

	@Message(id = ID_OFFSET + 44, value = "Unable to shut down the Elasticsearch client: %1$s")
	SearchException unableToShutdownClient(String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 45, value = "No built-in index field type for class: '%1$s'.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET + 53,
			value = "Full-text features (analysis, fuzziness) are not supported for fields of this type.")
	SearchException fullTextFeaturesNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET + 54,
			value = "Incomplete field definition."
					+ " You must call toReference() to complete the field definition.")
	SearchException incompleteFieldDefinition(@Param EventContext context);

	@Message(id = ID_OFFSET + 55,
			value = "Multiple calls to toReference() for the same field definition."
					+ " You must call toReference() exactly once.")
	SearchException cannotCreateReferenceMultipleTimes(@Param EventContext context);

	@Message(id = ID_OFFSET + 56, value = "Invalid Elasticsearch version: '%1$s'."
			+ " Expected format is 'x.y.z-qualifier', where 'x', 'y' and 'z' are integers,"
			+ " and 'qualifier' is an string of word characters (alphanumeric or '_')."
			+ " Incomplete versions are allowed, for example '7.0' or just '7'.")
	SearchException invalidElasticsearchVersionWithoutDistribution(String invalidRepresentation,
			@Cause Throwable cause);

	@Message(id = ID_OFFSET + 57, value = "Invalid Elasticsearch version: '%1$s'."
			+ " Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier',"
			+ " where '<distribution>' is one of %2$s (defaults to '%3$s'),"
			+ " 'x', 'y' and 'z' are integers,"
			+ " and 'qualifier' is an string of word characters (alphanumeric or '_')."
			+ " Incomplete versions are allowed, for example 'elastic:7.0', '7.0' or just '7'.")
	SearchException invalidElasticsearchVersionWithOptionalDistribution(String invalidRepresentation,
			List<String> validDistributions, String defaultDistribution, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 58, value = "Invalid Elasticsearch distribution name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidElasticsearchDistributionName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 59, value = "Unexpected Elasticsearch version running on the cluster: '%2$s'."
			+ " Hibernate Search was configured for Elasticsearch '%1$s'.")
	SearchException unexpectedElasticsearchVersion(ElasticsearchVersion configuredVersion,
			ElasticsearchVersion actualVersion);

	@Message(id = ID_OFFSET + 60, value = "Cannot skip analysis on field '%1$s':"
			+ " the Elasticsearch backend will always normalize arguments before attempting matches on normalized fields.")
	SearchException skipAnalysisOnNormalizedField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 61,
			value = "Ambiguous Elasticsearch version: '%s'."
					+ " This version matches multiple dialects."
					+ " Please use a more precise version to remove the ambiguity." )
	SearchException ambiguousElasticsearchVersion(ElasticsearchVersion version);

	@Message(id = ID_OFFSET + 62,
			value = "Invalid index field type: both null token '%2$s' ('indexNullAs')"
					+ " and analyzer '%1$s' are assigned to this type."
					+ " 'indexNullAs' is not supported on analyzed fields.")
	SearchException cannotUseIndexNullAsAndAnalyzer(String analyzerName, String indexNullAs, @Param EventContext context);

	@Message(id = ID_OFFSET + 63,
			value = "Multiple values assigned to field '%1$s': this field is single-valued."
					+ " Declare the field as multi-valued in order to allow this.")
	SearchException multipleValuesForSingleValuedField(String absolutePath);

	@Message(id = ID_OFFSET + 64,
			value = "Invalid use of explain(Object id) on a query targeting multiple types."
					+ " Use explain(String typeName, Object id) and pass one of %1$s as the type name." )
	SearchException explainRequiresTypeName(Set<String> targetedTypeNames);

	@Message(id = ID_OFFSET + 65,
			value = "Invalid mapped type name: '%2$s'."
					+ " This type is not among the mapped types targeted by this query: %1$s.")
	SearchException explainRequiresTypeTargetedByQuery(Set<String> targetedTypeNames, String typeName);

	@Message(id = ID_OFFSET + 66,
			value = "Invalid document identifier: '%2$s'. No such document in index '%1$s'.")
	SearchException explainUnknownDocument(URLEncodedString indexName, URLEncodedString id);

	@Message(id = ID_OFFSET + 67, value = "Invalid index field type: missing decimal scale."
			+ " Define the decimal scale explicitly.  %1$s")
	SearchException nullDecimalScale(String hint, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 69,
			value = "Unable to encode value '%1$s': this field type only supports values ranging from '%2$s' to '%3$s'."
					+ " If you want to encode values that are outside this range, change the decimal scale for this field."
					+ " Do not forget to reindex all your data after changing the decimal scale.")
	SearchException scaledNumberTooLarge(Number value, Number min, Number max);

	@Message(id = ID_OFFSET + 70,
			value = "Invalid index field type: decimal scale '%1$s' is positive."
						+ " The decimal scale of BigInteger fields must be zero or negative.")
	SearchException invalidDecimalScale(Integer decimalScale, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 72,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from a scope targeting indexes %3$s,"
					+ " but the given predicate was built from a scope targeting indexes %2$s.")
	SearchException predicateDefinedOnDifferentIndexes(SearchPredicate predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 73,
			value = "Invalid search sort: '%1$s'. You must build the sort from a scope targeting indexes %3$s,"
					+ " but the given sort was built from a scope targeting indexes %2$s.")
	SearchException sortDefinedOnDifferentIndexes(SearchSort sort, Set<String> sortIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 74,
			value = "Invalid search projection: '%1$s'. You must build the projection from a scope targeting indexes %3$s,"
					+ " but the given projection was built from a scope targeting indexes %2$s.")
	SearchException projectionDefinedOnDifferentIndexes(SearchProjection<?> projection, Set<String> projectionIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 76,
			value = "Invalid index field type: both analyzer '%1$s' and aggregations are enabled."
					+ " Aggregations are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not aggregable, and one with a normalizer that is aggregable.")
	SearchException cannotUseAnalyzerOnAggregableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 80,
			value = "Invalid range: '%1$s'. Elasticsearch range aggregations only accept ranges in the canonical form:"
					+ " (-Infinity, <value>) or [<value1>, <value2>) or [<value>, +Infinity)."
					+ " Call Range.canonical(...) to be sure to create such a range.")
	SearchException elasticsearchRangeAggregationRequiresCanonicalFormForRanges(Range<?> range);

	@Message(id = ID_OFFSET + 81,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherAggregations(SearchAggregation<?> aggregation);

	@Message(id = ID_OFFSET + 82,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from a scope targeting indexes %3$s,"
					+ " but the given aggregation was built from a scope targeting indexes %2$s.")
	SearchException aggregationDefinedOnDifferentIndexes(SearchAggregation<?> aggregation,
			Set<String> aggregationIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 85,
			value = "Duplicate aggregation definitions for key: '%1$s'")
	SearchException duplicateAggregationKey(@FormatWith(AggregationKeyFormatter.class) AggregationKey<?> key);

	@Message(id = ID_OFFSET + 87,
			value = "Invalid index field type: search analyzer '%1$s' is assigned to this type,"
				+ " but the indexing analyzer is missing."
				+ " Assign an indexing analyzer and a search analyzer, or remove the search analyzer.")
	SearchException searchAnalyzerWithoutAnalyzer(String searchAnalyzer, @Param EventContext context);

	@Message(id = ID_OFFSET + 88, value = "Call to the bulk REST API failed: %1$s")
	SearchException elasticsearchFailedBecauseOfBulkFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 89, value = "Invalid host/port: '%1$s'."
			+ " The host/port string must use the format 'host:port', for example 'mycompany.com:9200'"
			+ " The URI scheme ('http://', 'https://') must not be included.")
	SearchException invalidHostAndPort(String hostAndPort, @Cause Exception e);

	@Message(id = ID_OFFSET + 90, value = "Request execution exceeded the timeout of %1$s. Request was %2$s")
	SearchTimeoutException requestTimedOut(@FormatWith(DurationInSecondsAndFractionsFormatter.class) Duration timeout,
			@FormatWith(ElasticsearchRequestFormatter.class) ElasticsearchRequest request);

	@Message(id = ID_OFFSET + 91, value = "Invalid name for the type-name mapping strategy: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidTypeNameMappingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 92,
			value = "Missing field '%1$s' for one of the search hits."
					+ " The document was probably indexed with a different configuration: full reindexing is necessary.")
	SearchException missingTypeFieldInDocument(String fieldName);

	@Message(id = ID_OFFSET + 93,
			value = "Invalid Elasticsearch index layout:"
					+ " index names [%1$s, %2$s] resolve to multiple distinct indexes %3$s."
					+ " These names must resolve to a single index.")
	SearchException elasticsearchIndexNameAndAliasesMatchMultipleIndexes(URLEncodedString write, URLEncodedString read,
			Set<String> matchingIndexes);

	@Message(id = ID_OFFSET + 94,
			value = "Invalid Elasticsearch index layout:"
					+ " primary (non-alias) name for existing Elasticsearch index '%1$s'"
					+ " does not match the expected pattern '%2$s'.")
	SearchException invalidIndexPrimaryName(String elasticsearchIndexName, Pattern pattern);

	@Message(id = ID_OFFSET + 95,
			value = "Invalid Elasticsearch index layout:"
					+ " unique key '%1$s' extracted from the index name does not match any of %2$s.")
	SearchException invalidIndexUniqueKey(String uniqueKey, Set<String> knownKeys);

	@Message(id = ID_OFFSET + 96,
			value = "Invalid Elasticsearch index layout:"
					+ " the write alias and read alias are set to the same value: '%1$s'."
					+ " The write alias and read alias must be different." )
	SearchException sameWriteAndReadAliases(URLEncodedString writeAndReadAlias, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 97,
			value = "Missing or imprecise Elasticsearch version:"
					+ " when configuration property '%1$s' is set to 'false', "
					+ " the version is mandatory and must be at least as precise as 'x.y',"
					+ " where 'x' and 'y' are integers.")
	SearchException impreciseElasticsearchVersionWhenNoVersionCheck(String versionCheckPropertyKey);

	@Message(id = ID_OFFSET + 98, value = "The lifecycle strategy cannot be set at the index level anymore."
			+ " Set the schema management strategy via the property 'hibernate.search.schema_management.strategy' instead.")
	SearchException lifecycleStrategyMovedToMapper();

	@Message(id = ID_OFFSET + 100,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for fields in nested documents.")
	SearchException invalidSortModeAcrossNested(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 101,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for String fields."
					+ " Only MIN and MAX are supported.")
	SearchException invalidSortModeForStringField(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 102,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for temporal fields."
					+ " Only MIN, MAX, AVG and MEDIAN are supported.")
	SearchException invalidSortModeForTemporalField(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 103,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for a distance sort."
					+ " Only MIN, MAX, AVG and MEDIAN are supported.")
	SearchException invalidSortModeForDistanceSort(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 104,
			value = "Invalid sort filter: field '%1$s' is not contained in a nested object."
					+ " Sort filters are only available if the field to sort on is contained in a nested object.")
	SearchException cannotFilterSortOnRootDocumentField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 105,
			value = "Invalid search predicate: %1$s. This predicate targets fields %3$s,"
					+ " but only fields that are contained in the nested object with path '%2$s' are allowed here.")
	SearchException invalidNestedObjectPathForPredicate(SearchPredicate predicate, String nestedObjectPath,
			List<String> fieldPaths);

	@Message(id = ID_OFFSET + 106,
			value = "Invalid aggregation filter: field '%1$s' is not contained in a nested object."
					+ " Aggregation filters are only available if the field to aggregate on is contained in a nested object.")
	SearchException cannotFilterAggregationOnRootDocumentField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 107,
			value = "Duplicate index field template definition: '%1$s'."
					+ " Multiple bridges may be trying to access the same index field template, "
					+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.")
	SearchException indexSchemaFieldTemplateNameConflict(String name, @Param EventContext context);

	@Message(id = ID_OFFSET + 108,
			value = "Invalid value type. This field's values are of type '%1$s', which is not assignable from '%2$s'.")
	SearchException invalidFieldValueType(@FormatWith(ClassFormatter.class) Class<?> fieldValueType,
			@FormatWith(ClassFormatter.class) Class<?> invalidValueType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 109,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForIndexing(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 113,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is multi-valued."
					+ " Make sure to call '.multi()' when you create the projection.")
	SearchException invalidSingleValuedProjectionOnMultiValuedField(String absolutePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 117,
			value = "Implementation class differs: '%1$s' vs. '%2$s'.")
	SearchException differentImplementationClassForQueryElement(@FormatWith(ClassFormatter.class) Class<?> class1,
			@FormatWith(ClassFormatter.class) Class<?> class2);

	@Message(id = ID_OFFSET + 118,
			value = "Field codec differs: '%1$s' vs. '%2$s'.")
	SearchException differentFieldCodecForQueryElement(Object codec1, Object codec2);

	@Message(id = ID_OFFSET + 121, value = "Invalid dynamic type: '%1$s'."
			+ " Valid values are: %2$s.")
	SearchException invalidDynamicType(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 125,
			value = "Unable to update aliases for index '%1$s': %2$s")
	SearchException elasticsearchAliasUpdateFailed(Object indexName, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 126, value = "Invalid target hosts configuration:"
			+ " both the 'uris' property and the 'protocol' property are set."
			+ " Uris: '%1$s'. Protocol: '%2$s'."
			+ " Either set the protocol and hosts simultaneously using the 'uris' property,"
			+ " or set them separately using the 'protocol' property and the 'hosts' property.")
	SearchException uriAndProtocol(List<String> uris, String protocol);

	@Message(id = ID_OFFSET + 127, value = "Invalid target hosts configuration:"
			+ " both the 'uris' property and the 'hosts' property are set."
			+ " Uris: '%1$s'. Hosts: '%2$s'."
			+ " Either set the protocol and hosts simultaneously using the 'uris' property,"
			+ " or set them separately using the 'protocol' property and the 'hosts' property.")
	SearchException uriAndHosts(List<String> uris, List<String> hosts);

	@Message(id = ID_OFFSET + 128,
			value = "Invalid target hosts configuration: the 'uris' use different protocols (http, https)."
					+ " All URIs must use the same protocol. Uris: '%1$s'.")
	SearchException differentProtocolsOnUris(List<String> uris);

	@Message(id = ID_OFFSET + 129,
			value = "Invalid target hosts configuration: the list of hosts must not be empty.")
	SearchException emptyListOfHosts();

	@Message(id = ID_OFFSET + 130,
			value = "Invalid target hosts configuration: the list of URIs must not be empty.")
	SearchException emptyListOfUris();

	@Message(id = ID_OFFSET + 131, value = "Unable to find the given custom index settings file: '%1$s'.")
	SearchException customIndexSettingsFileNotFound(String filePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 132, value = "Error on loading the given custom index settings file '%1$s': %2$s")
	SearchException customIndexSettingsErrorOnLoading(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 133, value = "There are some JSON syntax errors on the given custom index settings file '%1$s': %2$s")
	SearchException customIndexSettingsJsonSyntaxErrors(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 134, value = "Invalid use of 'missing().first()' for an ascending distance sort. Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized.")
	SearchException missingFirstOnAscSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 135, value = "Invalid use of 'missing().last()' for a descending distance sort. Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized.")
	SearchException missingLastOnDescSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 136, value = "Invalid use of 'missing().use(...)' for a distance sort. Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized.")
	SearchException missingAsOnSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 137, value = "The index schema named predicate '%1$s' was added twice.")
	SearchException indexSchemaNamedPredicateNameConflict(String relativeFilterName, @Param EventContext context);

	@Message(id = ID_OFFSET + 138,
			value = "Predicate definition differs: '%1$s' vs. '%2$s'.")
	SearchException differentPredicateDefinitionForQueryElement(Object predicateDefinition1, Object predicateDefinition2);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET + 140, value = "A search query fetching all hits was requested," +
			" but only '%2$s' hits were retrieved because the maximum result window size forces a limit of '%1$s'" +
			" hits. Refer to Elasticsearch's 'max_result_window_size' setting for more information." )
	void defaultedLimitedHits(Integer defaultLimit, long hitCount);

	@Message(id = ID_OFFSET + 141,
			value = "Incompatible Elasticsearch version:"
					+ " version '%2$s' does not match version '%1$s' that was provided"
					+ " when the backend was created."
					+ " You can provide a more precise version on startup,"
					+ " but you cannot override the version that was provided when the backend was created.")
	SearchException incompatibleElasticsearchVersionOnStart(ElasticsearchVersion versionOnCreation,
			ElasticsearchVersion versionOnStart);

	@Message(id = ID_OFFSET + 148,
			value = "Invalid backend configuration: mapping requires multi-tenancy"
					+ " but no multi-tenancy strategy is set.")
	SearchException multiTenancyRequiredButExplicitlyDisabledByBackend();

	@Message(id = ID_OFFSET + 149,
			value = "Invalid backend configuration: mapping requires single-tenancy"
					+ " but multi-tenancy strategy is set.")
	SearchException multiTenancyNotRequiredButExplicitlyEnabledByTheBackend();

	@Message(id = ID_OFFSET + 150,
			value = "Param with name '%1$s' has not been defined for the named predicate '%2$s'.")
	SearchException paramNotDefined(String name, String predicateName, @Param EventContext context);

	@Message(id = ID_OFFSET + 151, value = "Unable to find the given custom index mapping file: '%1$s'.")
	SearchException customIndexMappingFileNotFound(String filePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 152, value = "Error on loading the given custom index mapping file '%1$s': %2$s")
	SearchException customIndexMappingErrorOnLoading(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 153, value = "There are some JSON syntax errors on the given custom index mapping file '%1$s': %2$s")
	SearchException customIndexMappingJsonSyntaxErrors(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 154,
			value = "Invalid context for projection on field '%1$s': the surrounding projection"
					+ " is executed for each object in field '%2$s', which is not a parent of field '%1$s'."
					+ " Check the structure of your projections.")
	SearchException invalidContextForProjectionOnField(String absolutePath,
			String objectFieldAbsolutePath);

	@Message(id = ID_OFFSET + 155,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is effectively multi-valued in this context,"
					+ " because parent object field '%2$s' is multi-valued."
					+ " Either call '.multi()' when you create the projection on field '%1$s',"
					+ " or wrap that projection in an object projection like this:"
					+ " 'f.object(\"%2$s\").from(<the projection on field %1$s>).as(...).multi()'.")
	SearchException invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(String absolutePath,
			String objectFieldAbsolutePath);

	@Message(id = ID_OFFSET + 156,
			value = "Unexpected mapped type name extracted from hits: '%1$s'. Expected one of: %2$s."
					+ " The document was probably indexed with a different configuration: full reindexing is necessary.")
	SearchException unexpectedMappedTypeNameForByMappedTypeProjection(String typeName, Set<String> expectedTypeNames);

	@Message(id = ID_OFFSET + 157, value = "Unable to export the schema for '%1$s' index: %2$s" )
	SearchException unableToExportSchema(String indexName, String message, @Cause IOException e);

	@Message(id = ID_OFFSET + 158, value = "Invalid use of 'missing().lowest()' for an ascending distance sort. " +
			"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized. ")
	SearchException missingLowestOnAscSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 159, value = "Invalid use of 'missing().lowest()' for a descending distance sort. " +
			"Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized. ")
	SearchException missingLowestOnDescSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 160,
			value = "Invalid highlighter: '%1$s'. You must build the highlighter from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherQueryHighlighters(SearchHighlighter highlighter);

	@Message(id = ID_OFFSET + 161,
			value = "Invalid highlighter: '%1$s'. You must build the highlighter from a scope targeting indexes %3$s,"
					+ " but the given highlighter was built from a scope targeting indexes %2$s.")
	SearchException queryHighlighterDefinedOnDifferentIndexes(SearchHighlighter highlighter, Set<String> configurationIndexes, Set<String> scopeIndexes);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 162,
			value = "No fields were added to be highlighted, but some query level highlighters were provided. " +
					"These highlighters will be ignored.")
	void noFieldsToHighlight();

	@Message(id = ID_OFFSET + 163,
			value = "Cannot find a highlighter with name '%1$s'." +
					" Available highlighters are: %2$s." +
					" Was it configured with `highlighter(\"%1$s\", highlighterContributor)`?")
	SearchException cannotFindHighlighter(String highlighterName, Set<String> highlighters);

	@Message(id = ID_OFFSET + 164, value = "Named highlighters cannot use a blank string as name.")
	SearchException highlighterNameCannotBeBlank();

	@Message(id = ID_OFFSET + 165,
			value = "Highlighter with name '%1$s' is already defined. Use a different name to add another highlighter.")
	SearchException highlighterWithTheSameNameCannotBeAdded(String highlighterName);

	@Message(id = ID_OFFSET + 166,
			value = "'%1$s' highlighter type cannot be applied to '%2$s' field. " +
					"'%2$s' must have either 'ANY' or '%1$s' among the configured highlightable values.")
	SearchException highlighterTypeNotSupported(SearchHighlighterType type, String field);

	@Message(id = ID_OFFSET + 167,
			value = "Cannot use 'NO' in combination with other highlightable values. Applied values are: '%1$s'")
	SearchException unsupportedMixOfHighlightableValues(Set<Highlightable> highlightable);

	@Message(id = ID_OFFSET + 168,
			value = "The '%1$s' term vector storage strategy is not compatible with the fast vector highlighter. " +
					"Either change the strategy to one of `WITH_POSITIONS_PAYLOADS`/`WITH_POSITIONS_OFFSETS_PAYLOADS` or remove the requirement for the fast vector highlighter support.")
	SearchException termVectorDontAllowFastVectorHighlighter(TermVector termVector);

	@Message(id = ID_OFFSET + 169,
			value = "Setting the `highlightable` attribute to an empty array is not supported. " +
					"Set the value to `NO` if the field does not require the highlight projection.")
	SearchException noHighlightableProvided();

	@Message(id = ID_OFFSET + 170,
			value = "Highlight projection cannot be applied within nested context of '%1$s'.")
	SearchException cannotHighlightInNestedContext(String currentNestingField,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET + 171,
			value = "The highlight projection cannot be applied to a field from an object using `ObjectStructure.NESTED` structure.")
	SearchException cannotHighlightFieldFromNestedObjectStructure(@Param EventContext eventContext);

	@Message(id = ID_OFFSET + 172, value = "'%1$s' cannot be nested in an object projection. "
			+ "%2$s")
	SearchException cannotUseProjectionInNestedContext(String projection, String hint, @Param EventContext eventContext);
}
