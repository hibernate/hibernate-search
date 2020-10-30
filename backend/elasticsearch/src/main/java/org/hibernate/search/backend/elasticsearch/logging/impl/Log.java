/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.logging.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.logging.spi.AggregationKeyFormatter;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.common.SortMode;
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
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (ES module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ES = MessageConstants.BACKEND_ES_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_LEGACY_ES + 7,
			value = "Elasticsearch request failed: %3$s\nRequest: %1$s\nResponse: %2$s"
	)
	SearchException elasticsearchRequestFailed(
			@FormatWith( ElasticsearchRequestFormatter.class ) ElasticsearchRequest request,
			@FormatWith( ElasticsearchResponseFormatter.class ) ElasticsearchResponse response,
			String causeMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 8,
			// Note: no need to add a '\n' before "Response", since the formatter will always add one
			value = "Elasticsearch bulked request failed: %3$s\nRequest metadata: %1$sResponse: %2$s"
	)
	SearchException elasticsearchBulkedRequestFailed(
			@FormatWith( ElasticsearchJsonObjectFormatter.class ) JsonObject requestMetadata,
			@FormatWith( ElasticsearchJsonObjectFormatter.class ) JsonObject response,
			String causeMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ES + 10,
			value = "Elasticsearch connection time-out; check the cluster status, it should be 'green'" )
	SearchException elasticsearchRequestTimeout();

	@Message(id = ID_OFFSET_LEGACY_ES + 20,
			value = "Could not create mapping for index '%1$s': %2$s"
	)
	SearchException elasticsearchMappingCreationFailed(String indexName, String causeMessage, @Cause Exception cause);

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
			value = "Missing index: index aliases [%1$s, %2$s] do not point to any index in the Elasticsearch cluster." )
	SearchException indexMissing(URLEncodedString write, URLEncodedString read);

	@LogMessage(level = Level.DEBUG)
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
			value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s and %d objects in payload in %dms."
					+ " Response had status %d '%s'.")
	void executedRequest(String method, String path, Map<String, String> getParameters, int bodyParts, long timeInMs,
			int responseStatusCode, String responseStatusMessage);

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
			value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s and %d objects in payload in %dms."
					+ " Response had status %d '%s'. Request body: <%s>. Response body: <%s>")
	void executedRequest(String method, String path, Map<String, String> getParameters, int bodyParts, long timeInMs,
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

	@Message(id = ID_OFFSET + 4,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForSearch(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 6,
			value = "Invalid target for Elasticsearch extension: '%1$s'."
					+ " This extension can only be applied to components created by an Elasticsearch backend.")
	SearchException elasticsearchExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET + 8,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = ID_OFFSET + 10,
			value = "Invalid target field: object field '%1$s' is flattened."
					+ " If you want to use a 'nested' predicate on this field, set its structure to 'NESTED'."
					+ " Do not forget to reindex all your data after changing the structure.")
	SearchException nonNestedFieldForNestedQuery(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 11,
			value = "Invalid search sort: '%1$s'. You must build the sort from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = ID_OFFSET + 14,
			value = "Invalid backend configuration: index '%1$s' requires multi-tenancy"
					+ " but no multi-tenancy strategy is set.")
	SearchException multiTenancyRequiredButNotSupportedByBackend(String indexName, @Param EventContext context);

	@Message(id = ID_OFFSET + 15, value = "Invalid multi-tenancy strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 16,
			value = "Invalid tenant identifier: '%1$s'."
					+ " The tenant identifier must be null, because multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(String tenantId, @Param EventContext context);

	@Message(id = ID_OFFSET + 17,
			value = "Missing tenant identifier."
					+ " The tenant identifier must be non-null, because multi-tenancy is enabled for this backend.")
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

	@Message(id = ID_OFFSET + 41,
			value = "Inconsistent configuration for field '%1$s' in a search query across multiple indexes: %2$s")
	SearchException inconsistentConfigurationForFieldForSearch(String absoluteFieldPath, String causeMessage,
			@Param EventContext context, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 44, value = "Unable to shut down the Elasticsearch client: %1$s")
	SearchException unableToShutdownClient(String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 45, value = "No built-in index field type for class: '%1$s'.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET + 49,
			value = "Inconsistent configuration for the identifier in a search query across multiple indexes: converter differs: '%1$s' vs. '%2$s'.")
	SearchException inconsistentConfigurationForIdentifierForSearch(ToDocumentIdentifierValueConverter<?> component1,
			ToDocumentIdentifierValueConverter<?> component2, @Param EventContext context);

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
			+ " The version must be in the form 'x.y.z-qualifier', where 'x', 'y' and 'z' are integers,"
			+ " and 'qualifier' is an string of word characters (alphanumeric or '_')."
			+ " Incomplete versions are allowed, for example '7.0' or just '7'.")
	SearchException invalidElasticsearchVersion(String versionString);

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
			+ " Define the decimal scale explicitly.")
	SearchException nullDecimalScale(@Param EventContext eventContext);

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
					+ " index aliases [%1$s, %2$s] resolve to multiple distinct indexes %3$s."
					+ " These aliases must resolve to a single index.")
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

	@Message(id = ID_OFFSET + 97, value = "Invalid Elasticsearch version: '%1$s'."
			+ " When version_check.enabled is set to false, "
			+ " the version must at least be in the form 'x.y', where 'x' and 'y' are integers")
	SearchException invalidElasticsearchVersionCheckConfiguration(String versionString);

	@Message(id = ID_OFFSET + 98, value = "The lifecycle strategy cannot be set at the index level anymore."
			+ " Set the schema management strategy via the property 'hibernate.search.schema_management.strategy' instead.")
	SearchException lifecycleStrategyMovedToMapper();

	@Message(id = ID_OFFSET + 99,
			value = "Invalid target fields for simple-query-string predicate:"
					+ " fields [%1$s, %3$s] are in different nested documents [%2$s, %4$s]."
					+ " All fields targeted by a simple-query-string predicate must be in the same document.")
	SearchException simpleQueryStringSpanningMultipleNestedPaths(String fieldPath1, String nestedPath1,
			String fieldPath2, String nestedPath2);

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

	@Message(id = ID_OFFSET + 110,
			value = "Invalid type: the index root is not an object field.")
	SearchException invalidIndexElementTypeRootIsNotObjectField();

	@Message(id = ID_OFFSET + 111,
			value = "Invalid type: '%1$s' is a value field, not an object field.")
	SearchException invalidIndexElementTypeValueFieldIsNotObjectField(String absolutePath);

	@Message(id = ID_OFFSET + 112,
			value = "Invalid type: '%1$s' is an object field, not a value field.")
	SearchException invalidIndexElementTypeObjectFieldIsNotValueField(String absolutePath);

	@Message(id = ID_OFFSET + 113,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is multi-valued."
					+ " Make sure to call '.multi()' when you create the projection.")
	SearchException invalidSingleValuedProjectionOnMultiValuedField(String absolutePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 114, value = "Cannot use '%2$s' on field '%1$s'."
			+ " Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant)."
			+ " If it already is, then '%2$s' is not available for fields of this type.")
	SearchException cannotUseQueryElementForField(String absoluteFieldPath, String queryElementName, @Param EventContext context);

	@Message(id = ID_OFFSET + 115,
			value = "Inconsistent support for '%1$s': %2$s")
	SearchException inconsistentSupportForQueryElement(String queryElementName,
			String causeMessage, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 116,
			value = "Field attribute '%1$s' differs: '%2$s' vs. '%3$s'.")
	SearchException differentFieldAttribute(String attributeName, Object component1, Object component2);

	@Message(id = ID_OFFSET + 117,
			value = "Implementation class differs: '%1$s' vs. '%2$s'.")
	SearchException differentImplementationClassForQueryElement(@FormatWith(ClassFormatter.class) Class<?> class1,
			@FormatWith(ClassFormatter.class) Class<?> class2);

	@Message(id = ID_OFFSET + 118,
			value = "Field codec differs: '%1$s' vs. '%2$s'.")
	SearchException differentFieldCodecForQueryElement(Object codec1, Object codec2);

	@Message(id = ID_OFFSET + 119,
			value = "'%1$s' can be used in some of the targeted indexes, but not all of them."
					+ " Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant) in all indexes,"
					+ " and that the field has the same type in all indexes.")
	SearchException partialSupportForQueryElement(String queryElementName);

	@Message(id = ID_OFFSET + 121, value = "Invalid dynamic type: '%1$s'."
			+ " Valid values are: %2$s.")
	SearchException invalidDynamicType(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 122,
			value = "Multiple conflicting models for field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingFieldModel(String absoluteFieldPath,
			AbstractElasticsearchIndexSchemaFieldNode fieldNode1, AbstractElasticsearchIndexSchemaFieldNode fieldNode2,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 123, value = "Cannot use '%2$s' on field '%1$s'."
			+ " '%2$s' is not available for object fields.")
	SearchException cannotUseQueryElementForObjectField(String absoluteFieldPath, String queryElementName, @Param EventContext context);

	@Message(id = ID_OFFSET + 124, value = "Cannot use '%2$s' on field '%1$s': %3$s")
	SearchException cannotUseQueryElementForObjectFieldBecauseCreationException(String absoluteFieldPath,
			String queryElementName, String causeMessage, @Cause SearchException cause, @Param EventContext context);

}
