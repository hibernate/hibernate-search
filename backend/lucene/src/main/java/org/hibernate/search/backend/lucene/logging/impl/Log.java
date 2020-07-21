/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.logging.spi.AggregationKeyFormatter;
import org.hibernate.search.util.common.logging.impl.DurationInSecondsAndFractionsFormatter;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
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

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.BACKEND_LUCENE_ID_RANGE_MIN, max = MessageConstants.BACKEND_LUCENE_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5
		@ValidIdRange(min = 35, max = 35),
		@ValidIdRange(min = 41, max = 41),
		@ValidIdRange(min = 52, max = 52),
		@ValidIdRange(min = 55, max = 55),
		@ValidIdRange(min = 75, max = 75),
		@ValidIdRange(min = 114, max = 114),
		@ValidIdRange(min = 118, max = 118),
		@ValidIdRange(min = 225, max = 225),
		@ValidIdRange(min = 226, max = 226),
		@ValidIdRange(min = 228, max = 228),
		@ValidIdRange(min = 265, max = 265),
		@ValidIdRange(min = 274, max = 274),
		@ValidIdRange(min = 284, max = 284),
		@ValidIdRange(min = 320, max = 320),
		@ValidIdRange(min = 321, max = 321),
		@ValidIdRange(min = 329, max = 329),
		@ValidIdRange(min = 330, max = 330),
		@ValidIdRange(min = 337, max = 337),
		@ValidIdRange(min = 341, max = 341),
		@ValidIdRange(min = 342, max = 342),
		@ValidIdRange(min = 344, max = 344),
		@ValidIdRange(min = 345, max = 345),
		@ValidIdRange(min = 353, max = 353)
		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5.
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_1 + 41, value = "Index directory not found, creating: '%1$s'")
	void indexDirectoryNotFoundCreatingNewOne(Path absolutePath);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_1 + 52,
			value = "Going to reset the index writer and force release of the IndexWriter lock. %1$s")
	void indexWriterReset(@FormatWith(EventContextFormatter.class) EventContext context);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_1 + 55,
			value = "Unable to close the index reader. %1$s")
	void unableToCloseIndexReader(@FormatWith(EventContextFormatter.class) EventContext context, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_1 + 75,
			value = "Configuration setting '%1$s' was not specified: using LATEST (currently '%2$s'). %3$s")
	void recommendConfiguringLuceneVersion(String key, Version latest, @FormatWith(EventContextFormatter.class) EventContext context);

	@Message(id = ID_OFFSET_1 + 114,
			value = "Could not load resource: '%1$s'")
	SearchException unableToLoadResource(String fileName);

	@Message(id = ID_OFFSET_1 + 118,
			value = "Index Merge operation on index '%1$s'")
	String indexMergeOperation(String indexName);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_1 + 225,
			value = "An index locking error occurred during initialization of Directory '%s'."
					+ " This might indicate a concurrent initialization;"
					+ " If you experience errors on this index you might need to remove the lock, or rebuild the index."
	)
	void lockingFailureDuringInitialization(String directoryDescription, @Param EventContext context);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET_1 + 226, value = "%s: %s" )
	void logInfoStreamMessage(String componentName, String message);

	@Message(id = ID_OFFSET_1 + 228,
			value = "Value '%1$ss' is not in a valid format to express a Lucene version: %2$s" )
	SearchException illegalLuceneVersionFormat(String property, String luceneErrorMessage, @Cause Exception e);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET_1 + 274, value = "Executing Lucene query '%s'" )
	void executingLuceneQuery(Query luceneQuery);

	@Message(id = ID_OFFSET_1 + 284,
			value = "An exception occurred while opening multiple indexes." )
	SearchException failureOnMultiReaderRefresh(@Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_1 + 329,
			value = "Error while applying analysis configuration: %1$s")
	SearchException unableToApplyAnalysisConfiguration(String errorMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_1 + 330,
			value = "Multiple analyzer definitions with the same name: '%1$s'. The analyzer names must be unique." )
	SearchException analyzerDefinitionNamingConflict(String analyzerDefinitionName);

	@Message(id = ID_OFFSET_1 + 337,
			value = "Multiple parameters with the same name: '%1$s'. Can't assign both value '%2$s' and '%3$s'" )
	SearchException analysisComponentParameterConflict(String name, String value1, String value2);

	@Message(id = ID_OFFSET_1 + 341,
			value = "Multiple normalizer definitions with the same name: '%1$s'. The normalizer names must be unique." )
	SearchException normalizerDefinitionNamingConflict(String normalizerDefinitionName);

	@Message(id = ID_OFFSET_1 + 342,
			value = "Cannot apply both an analyzer and a normalizer. Analyzer: '%1$s', normalizer: '%2$s'.")
	SearchException cannotApplyAnalyzerAndNormalizer(String analyzerName, String normalizerName, @Param EventContext context);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_1 + 344,
			value = "The normalizer for definition '%s' produced %d tokens."
			+ " Normalizers should never produce more than one token."
			+ " The tokens have been concatenated by Hibernate Search,"
			+ " but you should fix your normalizer definition." )
	void normalizerProducedMultipleTokens(String normalizerName, int token);

	@Message(id = ID_OFFSET_1 + 345,
			value = "Cannot apply an analyzer on a sortable field. Use a normalizer instead. Analyzer: '%1$s'."
			+ " If an actual analyzer (with tokenization) is necessary, define two separate fields:"
			+ " one with an analyzer that is not sortable, and one with a normalizer that is sortable.")
	SearchException cannotUseAnalyzerOnSortableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET_1 + 353,
			value = "Unknown analyzer: '%1$s'. Make sure you defined this analyzer.")
	SearchException unknownAnalyzer(String analyzerName, @Param EventContext context);

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 here

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.BACKEND_LUCENE_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_2 + 0,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForSearch(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 1,
			value = "Path '%1$s' exists but does not point to a writable directory.")
	SearchException pathIsNotWriteableDirectory(Path rootDirectory);

	@Message(id = ID_OFFSET_2 + 2,
			value = "Unable to initialize root directory '%1$s': %2$s")
	SearchException unableToInitializeRootDirectory(Path rootDirectory, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 5,
			value = "The Lucene extension can only be applied to objects"
			+ " derived from the Lucene backend. Was applied to '%1$s' instead.")
	SearchException luceneExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET_2 + 10,
			value = "A Lucene query cannot include search predicates built using a non-Lucene search scope."
			+ " Given predicate was: '%1$s'")
	SearchException cannotMixLuceneSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = ID_OFFSET_2 + 12,
			value = "Field '%1$s' is not an object field.")
	SearchException nonObjectFieldForNestedQuery(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 13,
			value = "Object field '%1$s' is flattened: its structure was lost upon indexing and 'nested' features are not available.")
	SearchException nonNestedFieldForNestedQuery(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 14,
			value = "A Lucene query cannot include search sorts built using a non-Lucene search scope."
			+ " Given sort was: '%1$s'")
	SearchException cannotMixLuceneSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = ID_OFFSET_2 + 15,
			value = "Unable to initialize index directory: %1$s")
	SearchException unableToInitializeIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 16, value = "Unable to index entity of type '%2$s' with identifier '%3$s' and tenant identifier '%1$s'.")
	SearchException unableToIndexEntry(String tenantId, String entityTypeName, Object entityIdentifier,
			@Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 17,
			value = "Unable to delete entity of type '%2$s' with identifier '%3$s' and tenant identifier '%1$s'.")
	SearchException unableToDeleteEntryFromIndex(String tenantId, String entityTypeName, Object entityIdentifier,
			@Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 19,
			value = "Unable to commit.")
	SearchException unableToCommitIndex(@Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 24,
			value = "A multi-index scope cannot include both a Lucene index and another type of index."
					+ " Base scope was: '%1$s', Lucene index was: '%2$s'")
	SearchException cannotMixLuceneScopeWithOtherType(IndexScopeBuilder baseScope,
			LuceneIndexManager luceneIndex, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 25,
			value = "A multi-index scope cannot span multiple Lucene backends."
					+ " Base scope was: '%1$s', index from another backend was: '%2$s'")
	SearchException cannotMixLuceneScopeWithOtherBackend(IndexScopeBuilder baseScope,
			LuceneIndexManager indexFromOtherBackend, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 27,
			value = "An IOException happened while executing the query '%1$s'.")
	SearchException ioExceptionOnQueryExecution(Query luceneQuery, @Param EventContext context, @Cause IOException e);

	@Message(id = ID_OFFSET_2 + 29,
			value = "Index '%1$s' requires multi-tenancy but the backend does not support it in its current configuration.")
	SearchException multiTenancyRequiredButNotSupportedByBackend(String indexName, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 30,
			value = "Invalid multi-tenancy strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_2 + 31,
			value = "Tenant identifier '%1$s' is provided, but multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(String tenantId, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 32,
			value = "Backend has multi-tenancy enabled, but no tenant identifier is provided.")
	SearchException multiTenancyEnabledButNoTenantIdProvided(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 33,
			value = "Attempt to unwrap a Lucene backend to '%1$s',"
					+ " but this backend can only be unwrapped to '%2$s'.")
	SearchException backendUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 34,
			value = "The index schema node '%1$s' was added twice."
					+ " Multiple bridges may be trying to access the same index field, "
					+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.")
	SearchException indexSchemaNodeNameConflict(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 39,
			value = "Invalid field reference for this document element: this document element has path '%1$s', but the referenced field has a parent with path '%2$s'.")
	SearchException invalidFieldForDocumentElement(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET_2 + 44,
			value = "Computed minimum for minimumShouldMatch constraint is out of bounds:"
					+ " expected a number between '1' and '%1$s', got '%2$s'.")
	SearchException minimumShouldMatchMinimumOutOfBounds(int totalShouldClauseNumber, int minimum);

	@Message(id = ID_OFFSET_2 + 45,
			value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ignoreConstraintCeiling);

	@Message(id = ID_OFFSET_2 + 48,
			value = "This native field does not support projection.")
	SearchException unsupportedProjectionForNativeField(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 49,
			value = "Invalid field path; expected path '%1$s', got '%2$s'.")
	SearchException invalidFieldPath(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET_2 + 50,
			value = "Unable to convert DSL parameter: %1$s")
	SearchException cannotConvertDslParameter(String errorMessage, @Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 51,
			value = "Attempt to unwrap a Lucene index manager to '%1$s',"
					+ " but this index manager can only be unwrapped to '%2$s'.")
	SearchException indexManagerUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 52,
			value = "Unable to create analyzer for name '%1$s'.")
	SearchException unableToCreateAnalyzer(String name, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 53,
			value = "Unable to create normalizer for name '%1$s'.")
	SearchException unableToCreateNormalizer(String name, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 54,
			value = "Unknown normalizer: '%1$s'. Make sure you defined this normalizer.")
	SearchException unknownNormalizer(String normalizerName, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 55,
			value = "A Lucene query cannot include search projections built using a non-Lucene search scope."
			+ " Given projection was: '%1$s'")
	SearchException cannotMixLuceneSearchQueryWithOtherProjections(SearchProjection<?> projection);

	@Message(id = ID_OFFSET_2 + 58,
			value = "Inconsistent configuration for field '%1$s' in a search query across multiple indexes: %2$s")
	SearchException inconsistentConfigurationForFieldForSearch(String absoluteFieldPath, String causeMessage,
			@Param EventContext context, @Cause SearchException cause);

	@Message(id = ID_OFFSET_2 + 61, value = "Failed to shut down the Lucene index manager.")
	SearchException failedToShutdownBackend(@Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 62, value = "Cannot guess field type for input type: '%1$s'.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 64, value = "Unexpected index: documentId '%1$s' was not collected." )
	SearchException documentIdNotCollected(Integer documentId);

	@Message(id = ID_OFFSET_2 + 67, value = "Unable to delete all entries matching query '%1$s'.")
	SearchException unableToDeleteAllEntriesFromIndex(Query query, @Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 68,
			value = "Inconsistent configuration for the identifier in a search query across multiple indexes: converter differs: '%1$s' vs. '%2$s'.")
	SearchException inconsistentConfigurationForIdentifierForSearch(ToDocumentIdentifierValueConverter<?> component1,
			ToDocumentIdentifierValueConverter<?> component2, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 69,
			value = "An IOException occurred while generating an Explanation.")
	SearchException ioExceptionOnExplain(@Cause IOException e);

	@Message(id = ID_OFFSET_2 + 70,
			value = "Full-text features (analysis, fuzziness) are not supported for fields of this type.")
	SearchException fullTextFeaturesNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 71,
			value = "Incomplete field definition."
					+ " You must call toReference() to complete the field definition.")
	SearchException incompleteFieldDefinition(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 72,
			value = "Multiple calls to toReference() for the same field definition."
					+ " You must call toReference() exactly once.")
	SearchException cannotCreateReferenceMultipleTimes(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 73, value = "Index-null-as option is not supported on analyzed field. Trying to define the analyzer: '%1$s' together with index null as: '%2$s'.")
	SearchException cannotUseIndexNullAsAndAnalyzer(String analyzerName, String indexNullAs, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 74,
			value = "Multiple values were added to single-valued field '%1$s'."
					+ " Declare the field as multi-valued in order to allow this."
	)
	SearchException multipleValuesForSingleValuedField(String absoluteFieldPath);

	// TODO HSEARCH-3899 avoiding merging index concepts (e.g.: documentId) and entity concepts (e.g.: mapped type)
	@Message(id = ID_OFFSET_2 + 75,
			value = "explain(String id) cannot be used when the query targets multiple mapped types."
					+ " Use explain(String typeName, String id) and pass one of %1$s as the mapped type name." )
	SearchException explainRequiresTypeName(Set<String> targetedTypeNames);

	@Message(id = ID_OFFSET_2 + 76,
			value = "The given mapped type name '%2$s' is not among the mapped types targeted by this query: %1$s." )
	SearchException explainRequiresTypeTargetedByQuery(Set<String> targetedTypeNames, String typeName);

	// TODO HSEARCH-3899 avoiding merging index concepts (e.g.: documentId) and entity concepts (e.g.: mapped type)
	@Message(id = ID_OFFSET_2 + 77,
			value = "Document with id '%2$s' does not exist for the mapped type '%1$s' and thus its match cannot be explained." )
	SearchException explainUnkownDocument(String typeName, String id);

	@Message(id = ID_OFFSET_2 + 78,
			value = "Unable to merge index segments.")
	SearchException unableToMergeSegments(@Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 79,
			value = "Unable to close the index writer after write failures.")
	SearchException unableToCloseIndexWriterAfterFailures(@Param EventContext context, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 80, value = "Impossible to detect a decimal scale to use for this field."
			+ " If the value is bridged, set '.asBigDecimal().decimalScale( int )' in the bind, else verify your mapping.")
	SearchException nullDecimalScale(@Param EventContext eventContext);

	@Message(id = ID_OFFSET_2 + 81, value = "The value '%1$s' cannot be indexed because its absolute value is too large.")
	SearchException scaledNumberTooLarge(Number value);

	@Message(id = ID_OFFSET_2 + 82, value = "Positive decimal scale ['%1$s'] is not allowed for BigInteger fields, since a BigInteger value cannot have any decimal digits.")
	SearchException invalidDecimalScale(Integer decimalScale, @Param EventContext eventContext);

	@Message(id = ID_OFFSET_2 + 84, value = "The predicate '%1$s' is defined on a scope targeting different indexes."
			+ " Predicate is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException predicateDefinedOnDifferentIndexes(SearchPredicate predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_2 + 85, value = "The sort '%1$s' is defined on a scope targeting different indexes."
			+ " Sort is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException sortDefinedOnDifferentIndexes(SearchSort predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_2 + 86, value = "The projection '%1$s' is defined on a scope targeting different indexes."
			+ " Projection is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException projectionDefinedOnDifferentIndexes(SearchProjection<?> predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_2 + 87,
			value = "Invalid filesystem access strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidFileSystemAccessStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_2 + 88,
			value = "Invalid locking strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidLockingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_2 + 89,
			value = "The sharding strategy '%1$s' is not implemented properly:"
					+ " it must call either context.setShardIdentifiers() or context.setShardingDisabled()"
					+ " in its initialize() method, but it did not." )
	SearchException missingShardIdentifiersAfterShardingStrategyInitialization(Object strategy);

	@Message(id = ID_OFFSET_2 + 90,
			value = "Missing value for property '%2$s'. The sharding strategy '%1$s' requires this property to be set.")
	SearchException missingPropertyValueForShardingStrategy(String strategyName, String propertyKey);

	@Message(id = ID_OFFSET_2 + 91,
			value = "Invalid routing key: '%1$s'. Valid keys are: %2$s.")
	SearchException invalidRoutingKeyForExplicitShardingStrategy(String invalidKey, Collection<String> validKeys);

	@Message(id = ID_OFFSET_2 + 94,
			value = "Cannot apply an analyzer on an aggregable field. Use a normalizer instead. Analyzer: '%1$s'."
					+ " If an actual analyzer (with tokenization) is necessary, define two separate fields:"
					+ " one with an analyzer that is not aggregable, and one with a normalizer that is aggregable.")
	SearchException cannotUseAnalyzerOnAggregableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 98,
			value = "An Lucene query cannot include search aggregations built using a non-Lucene search scope."
					+ " Given aggregation was: '%1$s'")
	SearchException cannotMixLuceneSearchQueryWithOtherAggregations(SearchAggregation<?> aggregation);

	@Message(id = ID_OFFSET_2 + 99, value = "The aggregation '%1$s' is defined on a scope targeting different indexes."
			+ " Aggregation is targeting: '%2$s'. Current scope is targeting: '%3$s'.")
	SearchException aggregationDefinedOnDifferentIndexes(SearchAggregation<?> aggregation,
			Set<String> aggregationIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET_2 + 102,
			value = "Multiple aggregations with the same key: '%1$s'")
	SearchException duplicateAggregationKey(@FormatWith(AggregationKeyFormatter.class) AggregationKey key);

	@Message(id = ID_OFFSET_2 + 104, value = "Cannot apply a search analyzer if an analyzer has not been defined on the same field." +
			" Search analyzer: '%1$s'.")
	SearchException searchAnalyzerWithoutAnalyzer(String searchAnalyzer, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 105,
			value = "Multiple conflicting models for object field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingObjectFieldModel(String absoluteFieldPath,
			LuceneIndexSchemaObjectFieldNode index1Model, LuceneIndexSchemaObjectFieldNode index2Model, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 106,
			value = "Multiple conflicting models for field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingFieldModel(String absoluteFieldPath,
			LuceneIndexSchemaObjectFieldNode objectNode, LuceneIndexSchemaFieldNode fieldNode, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 107, value = "Search query exceeded the timeout of %1$s: '%2$s'.")
	SearchTimeoutException timedOut(@FormatWith(DurationInSecondsAndFractionsFormatter.class) Duration timeout, String queryDescription);

	@Message(id = ID_OFFSET_2 + 108,
			value = "Invalid I/O strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidIOStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_2 + 109,
			value = "Index does not exist for directory '%1$s'")
	SearchException missingIndex(Directory directory, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 110,
			value = "Unable to validate index directory: %1$s")
	SearchException unableToValidateIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 111,
			value = "Unable to drop index directory: %1$s")
	SearchException unableToDropIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 112, value = "Multiple index conflicting models on nested document paths targeting '%1$s'. '%2$s' vs. '%3$s'.")
	SearchException conflictingNestedDocumentPathHierarchy(String absoluteFieldPath, List<String> nestedDocumentPathHierarchy1, List<String> nestedDocumentPathHierarchy2,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 113, value = "Simple query string targets fields [%1$s, %3$s] spanning multiple nested paths: %2$s, %4$s.")
	SearchException simpleQueryStringSpanningMultipleNestedPaths(String fieldPath1, String nestedPath1, String fieldPath2, String nestedPath2);

	@Message(id = ID_OFFSET_2 + 114,
			value = "Cannot compute the median across nested documents.")
	SearchException cannotComputeMedianAcrossNested(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 115,
			value = "Cannot compute the sum, average or median of a text field. Only min and max are supported.")
	SearchException cannotComputeSumOrAvgOrMedianForStringField(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 116,
			value = "Cannot compute the sum of a temporal field. Only min, max, avg and median are supported.")
	SearchException cannotComputeSumForTemporalField(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 117,
			value = "Cannot compute the sum for a distance sort. Only min, max, avg and median are supported.")
	SearchException cannotComputeSumForDistanceSort(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 118,
			value = "A failure occurred during a low-level write operation"
					+ " and the index writer had to be reset."
					+ " Some write operations may have been lost as a result."
					+ " Failure: %1$s")
	SearchException uncommittedOperationsBecauseOfFailure(String causeMessage,
			@Param EventContext context, @Cause Throwable cause);

	@Message(id = ID_OFFSET_2 + 120,
			value = "Field '%1$s' is not contained in a nested object."
					+ " Sort filters are only available if the field to sort on is contained in a nested object.")
	SearchException cannotFilterSortOnRootDocumentField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 121,
			value = "Predicate targets unexpected fields %2$s."
					+ " Only fields that are contained in the nested object with path '%1$s'"
					+ " are allowed here.")
	SearchException invalidNestedObjectPathForPredicate(String nestedObjectPath, List<String> fieldPaths);

	@Message(id = ID_OFFSET_2 + 122,
			value = "Field '%1$s' is not contained in a nested object."
					+ " Aggregation filters are only available if the field to aggregate on is contained in a nested object.")
	SearchException cannotFilterAggregationOnRootDocumentField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 123,
			value = "IndexWriter setting '%1$s' cannot be set to '%2$s': %3$s")
	SearchException illegalIndexWriterSetting(String settingName, Object settingValue, String message, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 124,
			value = "Merge policy setting '%1$s' cannot be set to '%2$s': %3$s")
	SearchException illegalMergePolicySetting(String settingName, Object settingValue, String message, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 125,
			value = "The index field template '%1$s' was added twice."
					+ " Multiple bridges may be trying to access the same index field template, "
					+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.")
	SearchException indexSchemaFieldTemplateNameConflict(String name, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 126,
			value = "Invalid value type. This field's values are of type '%1$s', which is not assignable from '%2$s'.")
	SearchException invalidFieldValueType(@FormatWith(ClassFormatter.class) Class<?> fieldValueType,
			@FormatWith(ClassFormatter.class) Class<?> invalidValueType,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 127,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForIndexing(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 128,
			value = "Invalid type: the index root is not an object field.")
	SearchException invalidIndexElementTypeRootIsNotObjectField();

	@Message(id = ID_OFFSET_2 + 129,
			value = "Invalid type: '%1$s' is a value field, not an object field.")
	SearchException invalidIndexElementTypeValueFieldIsNotObjectField(String absolutePath);

	@Message(id = ID_OFFSET_2 + 130,
			value = "Invalid type: '%1$s' is an object field, not a value field.")
	SearchException invalidIndexElementTypeObjectFieldIsNotValueField(String absolutePath);

	@Message(id = ID_OFFSET_2 + 131,
			value = "Projection on field '%1$s' cannot be single-valued, because this field is multi-valued."
					+ " Make sure to call '.multi()' when you create the projection.")
	SearchException invalidSingleValuedProjectionOnMultiValuedField(String absolutePath, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 132, value = "Cannot use '%2$s' on field '%1$s'."
			+ " Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant)."
			+ " If it already is, then '%2$s' is not available for fields of this type.")
	SearchException cannotUseQueryElementForField(String absoluteFieldPath, String queryElementName, @Param EventContext context);

	@Message(id = ID_OFFSET_2 + 133,
			value = "Inconsistent support for '%1$s': %2$s")
	SearchException inconsistentSupportForQueryElement(String queryElementName,
			String causeMessage, @Cause SearchException cause);

	@Message(id = ID_OFFSET_2 + 134,
			value = "Field attribute '%1$s' differs: '%2$s' vs. '%3$s'.")
	SearchException differentFieldAttribute(String attributeName, Object component1, Object component2);

	@Message(id = ID_OFFSET_2 + 135,
			value = "Implementation class differs: '%1$s' vs. '%2$s'.")
	SearchException differentImplementationClassForQueryElement(@FormatWith(ClassFormatter.class) Class<?> class1,
			@FormatWith(ClassFormatter.class) Class<?> class2);

	@Message(id = ID_OFFSET_2 + 136,
			value = "Field codec differs: '%1$s' vs. '%2$s'.")
	SearchException differentFieldCodecForQueryElement(Object codec1, Object codec2);

	@Message(id = ID_OFFSET_2 + 137,
			value = "'%1$s' can be used in some of the targeted indexes, but not all of them."
					+ " Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant) in all indexes,"
					+ " and that the field has the same type in all indexes.")
	SearchException partialSupportForQueryElement(String queryElementName);

}
