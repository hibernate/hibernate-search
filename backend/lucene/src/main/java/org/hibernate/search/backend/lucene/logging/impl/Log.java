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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchObjectFieldContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.logging.spi.AggregationKeyFormatter;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
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
		// Exceptions for legacy messages from Search 5 (engine module)
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
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ENGINE = MessageConstants.ENGINE_ID_RANGE_MIN;

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 41, value = "Index directory does not exist, creating: '%1$s'")
	void indexDirectoryNotFoundCreatingNewOne(Path absolutePath);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 52,
			value = "An index writer operation failed. Resetting the index writer and forcing release of locks. %1$s")
	void indexWriterResetAfterFailure(@FormatWith(EventContextFormatter.class) EventContext context);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 55,
			value = "Unable to close the index reader. %1$s")
	void unableToCloseIndexReader(@FormatWith(EventContextFormatter.class) EventContext context, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 75,
			value = "Missing value for configuration property '%1$s': using LATEST (currently '%2$s'). %3$s")
	void recommendConfiguringLuceneVersion(String key, Version latest, @FormatWith(EventContextFormatter.class) EventContext context);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 114,
			value = "Resource does not exist in classpath: '%1$s'")
	SearchException unableToLoadResource(String fileName);

	@Message(value = "Index Merge operation on index '%1$s'")
	String indexMergeOperation(String indexName);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 225,
			value = "Unable to acquire lock on the index while initializing directory '%s'."
					+ " Either the directory wasn't properly closed last time it was used due to a critical failure,"
					+ " or another instance of Hibernate Search is using it concurrently"
					+ " (which is not supported)."
					+ " If you experience indexing failures on this index"
					+ " you will need to remove the lock, and might need to rebuild the index.")
	void lockingFailureDuringInitialization(String directoryDescription, @Param EventContext context, @Cause Exception e);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 226, value = "%s: %s" )
	void logInfoStreamMessage(String componentName, String message);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 228,
			value = "Unable to parse '%1$ss' into a Lucene version: %2$s" )
	SearchException illegalLuceneVersionFormat(String property, String luceneErrorMessage, @Cause Exception e);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 274, value = "Executing Lucene query: %s" )
	void executingLuceneQuery(Query luceneQuery);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 284,
			value = "Unable to open index readers: %1$s" )
	SearchException unableToOpenIndexReaders(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 329,
			value = "Unable to apply analysis configuration: %1$s")
	SearchException unableToApplyAnalysisConfiguration(String errorMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 337,
			value = "Ambiguous value for parameter '%1$s': this parameter is set to two different values '%2$s' and '%3$s'.")
	SearchException analysisComponentParameterConflict(String name, String value1, String value2);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 342,
			value = "Invalid index field type: both analyzer '%1$s' and normalizer '%2$s' are assigned to this type."
					+ " Either an analyzer or a normalizer can be assigned, but not both.")
	SearchException cannotApplyAnalyzerAndNormalizer(String analyzerName, String normalizerName, @Param EventContext context);

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 344,
			value = "Invalid normalizer implementation: the normalizer for definition '%s' produced %d tokens."
					+ " Normalizers should never produce more than one token."
					+ " The tokens have been concatenated by Hibernate Search,"
					+ " but you should fix your normalizer definition.")
	void normalizerProducedMultipleTokens(String normalizerName, int token);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 345,
			value = "Invalid index field type: both analyzer '%1$s' and sorts are enabled."
					+ " Sorts are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not sortable, and one with a normalizer that is sortable.")
	SearchException cannotUseAnalyzerOnSortableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 353,
			value = "Unknown analyzer: '%1$s'. Make sure you defined this analyzer.")
	SearchException unknownAnalyzer(String analyzerName, @Param EventContext context);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.BACKEND_LUCENE_ID_RANGE_MIN;

	@Message(id = ID_OFFSET + 0,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForSearch(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 1,
			value = "Path '%1$s' exists but does not point to a writable directory.")
	SearchException pathIsNotWriteableDirectory(Path rootDirectory);

	@Message(id = ID_OFFSET + 5,
			value = "Invalid target for Lucene extension: '%1$s'."
					+ " This extension can only be applied to components created by a Lucene backend.")
	SearchException luceneExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET + 10,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = ID_OFFSET + 13,
			value = "Invalid target field: object field '%1$s' is flattened."
					+ " If you want to use a 'nested' predicate on this field, set its structure to 'NESTED'."
					+ " Do not forget to reindex all your data after changing the structure.")
	SearchException nonNestedFieldForNestedQuery(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 14,
			value = "Invalid search sort: '%1$s'. You must build the sort from a Lucene search scope.")
	SearchException cannotMixLuceneSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = ID_OFFSET + 15,
			value = "Unable to initialize index directory: %1$s")
	SearchException unableToInitializeIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 16, value = "Unable to index entity of type '%2$s' with identifier '%3$s' and tenant identifier '%1$s': %4$s")
	SearchException unableToIndexEntry(String tenantId, String entityTypeName, Object entityIdentifier,
			String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 17,
			value = "Unable to delete entity of type '%2$s' with identifier '%3$s' and tenant identifier '%1$s': %4$s")
	SearchException unableToDeleteEntryFromIndex(String tenantId, String entityTypeName, Object entityIdentifier,
			String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 19,
			value = "Unable to commit: %1$s")
	SearchException unableToCommitIndex(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 24,
			value = "Invalid multi-index scope: a scope cannot span both a Lucene index and another type of index."
					+ " Base scope: '%1$s', incompatible (Lucene) index: '%2$s'.")
	SearchException cannotMixLuceneScopeWithOtherType(IndexScopeBuilder baseScope,
			LuceneIndexManager luceneIndex, @Param EventContext context);

	@Message(id = ID_OFFSET + 25,
			value = "Invalid multi-index scope: a scope cannot span multiple Lucene backends."
					+ " Base scope: '%1$s', incompatible index (from another backend): '%2$s'.")
	SearchException cannotMixLuceneScopeWithOtherBackend(IndexScopeBuilder baseScope,
			LuceneIndexManager indexFromOtherBackend, @Param EventContext context);

	@Message(id = ID_OFFSET + 27,
			value = "Unable to execute search query '%1$s': %2$s")
	SearchException ioExceptionOnQueryExecution(Query luceneQuery, String causeMessage,
			@Param EventContext context, @Cause IOException cause);

	@Message(id = ID_OFFSET + 29,
			value = "Invalid backend configuration: index '%1$s' requires multi-tenancy"
					+ " but no multi-tenancy strategy is set.")
	SearchException multiTenancyRequiredButNotSupportedByBackend(String indexName, @Param EventContext context);

	@Message(id = ID_OFFSET + 30,
			value = "Invalid multi-tenancy strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 31,
			value = "Invalid tenant identifier: '%1$s'."
					+ " The tenant identifier must be null, because multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(String tenantId, @Param EventContext context);

	@Message(id = ID_OFFSET + 32,
			value = "Missing tenant identifier."
					+ " The tenant identifier must be non-null, because multi-tenancy is enabled for this backend.")
	SearchException multiTenancyEnabledButNoTenantIdProvided(@Param EventContext context);

	@Message(id = ID_OFFSET + 33,
			value = "Invalid requested type for this backend: '%1$s'."
					+ " Lucene backends can only be unwrapped to '%2$s'.")
	SearchException backendUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 34,
			value = "Duplicate index field definition: '%1$s'."
					+ " Index field names must be unique."
					+ " Look for two property mappings with the same field name,"
					+ " or two indexed-embeddeds with prefixes that lead to conflicting index field names,"
					+ " or two custom bridges declaring index fields with the same name.")
	SearchException indexSchemaNodeNameConflict(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 39,
			value = "Invalid field reference for this document element:"
					+ " this document element has path '%1$s', but the referenced field has a parent with path '%2$s'.")
	SearchException invalidFieldForDocumentElement(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET + 44,
			value = "Computed minimum for minimumShouldMatch constraint is out of bounds:"
					+ " expected a number between '1' and '%1$s', got '%2$s'.")
	SearchException minimumShouldMatchMinimumOutOfBounds(int totalShouldClauseNumber, int minimum);

	@Message(id = ID_OFFSET + 45,
			value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ignoreConstraintCeiling);

	@Message(id = ID_OFFSET + 48,
			value = "This native field does not support projection.")
	SearchException unsupportedProjectionForNativeField(@Param EventContext context);

	@Message(id = ID_OFFSET + 49,
			value = "Invalid field path; expected path '%1$s', got '%2$s'.")
	SearchException invalidFieldPath(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET + 50,
			value = "Unable to convert DSL argument: %1$s")
	SearchException cannotConvertDslParameter(String errorMessage, @Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET + 51,
			value = "Invalid requested type for this index manager: '%1$s'."
					+ " Lucene index managers can only be unwrapped to '%2$s'.")
	SearchException indexManagerUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass, @Param EventContext context);

	@Message(id = ID_OFFSET + 52,
			value = "Unable to create analyzer for name '%1$s': %2$s")
	SearchException unableToCreateAnalyzer(String name, String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 53,
			value = "Unable to create normalizer for name '%1$s': %2$s")
	SearchException unableToCreateNormalizer(String name, String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 54,
			value = "Unknown normalizer: '%1$s'. Make sure you defined this normalizer.")
	SearchException unknownNormalizer(String normalizerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 55,
			value = "Invalid search projection: '%1$s'. You must build the projection from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherProjections(SearchProjection<?> projection);

	@Message(id = ID_OFFSET + 58,
			value = "Inconsistent configuration for field '%1$s' in a search query across multiple indexes: %2$s")
	SearchException inconsistentConfigurationForFieldForSearch(String absoluteFieldPath, String causeMessage,
			@Param EventContext context, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 61, value = "Unable to shut down index accessor: %1$s")
	SearchException unableToShutdownIndexAccessor(String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 62, value = "No built-in index field type for class: '%1$s'.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET + 64, value = "Unexpected index: documentId '%1$s' was not collected." )
	SearchException documentIdNotCollected(Integer documentId);

	@Message(id = ID_OFFSET + 67, value = "Unable to delete all entries matching query '%1$s': %2$s")
	SearchException unableToDeleteAllEntriesFromIndex(Query query, String causeMessage, @Param EventContext context,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 68,
			value = "Inconsistent configuration for the identifier in a search query across multiple indexes: converter differs: '%1$s' vs. '%2$s'.")
	SearchException inconsistentConfigurationForIdentifierForSearch(ToDocumentIdentifierValueConverter<?> component1,
			ToDocumentIdentifierValueConverter<?> component2, @Param EventContext context);

	@Message(id = ID_OFFSET + 69,
			value = "Unable to explain search query: %1$s")
	SearchException ioExceptionOnExplain(String causeMessage, @Cause IOException cause);

	@Message(id = ID_OFFSET + 70,
			value = "Full-text features (analysis, fuzziness) are not supported for fields of this type.")
	SearchException fullTextFeaturesNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET + 71,
			value = "Incomplete field definition."
					+ " You must call toReference() to complete the field definition.")
	SearchException incompleteFieldDefinition(@Param EventContext context);

	@Message(id = ID_OFFSET + 72,
			value = "Multiple calls to toReference() for the same field definition."
					+ " You must call toReference() exactly once.")
	SearchException cannotCreateReferenceMultipleTimes(@Param EventContext context);

	@Message(id = ID_OFFSET + 73,
			value = "Invalid index field type: both null token '%2$s' ('indexNullAs')"
					+ " and analyzer '%1$s' are assigned to this type."
					+ " 'indexNullAs' is not supported on analyzed fields.")
	SearchException cannotUseIndexNullAsAndAnalyzer(String analyzerName, String indexNullAs, @Param EventContext context);

	@Message(id = ID_OFFSET + 74,
			value = "Multiple values assigned to field '%1$s': this field is single-valued."
					+ " Declare the field as multi-valued in order to allow this.")
	SearchException multipleValuesForSingleValuedField(String absoluteFieldPath);

	@Message(id = ID_OFFSET + 75,
			value = "Invalid use of explain(Object id) on a query targeting multiple types."
					+ " Use explain(String typeName, Object id) and pass one of %1$s as the type name." )
	SearchException explainRequiresTypeName(Set<String> targetedTypeNames);

	@Message(id = ID_OFFSET + 76,
			value = "Invalid mapped type name: '%2$s'."
					+ " This type is not among the mapped types targeted by this query: %1$s.")
	SearchException explainRequiresTypeTargetedByQuery(Set<String> targetedTypeNames, String typeName);

	@Message(id = ID_OFFSET + 77,
			value = "Invalid document identifier: '%2$s'. No such document for type '%1$s'.")
	SearchException explainUnknownDocument(String typeName, String id);

	@Message(id = ID_OFFSET + 78,
			value = "Unable to merge index segments: %1$s")
	SearchException unableToMergeSegments(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 79,
			value = "Unable to close the index writer after write failures: %1$s")
	SearchException unableToCloseIndexWriterAfterFailures(String causeMessage, @Param EventContext context,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 80, value = "Invalid index field type: missing decimal scale."
			+ " Define the decimal scale explicitly.")
	SearchException nullDecimalScale(@Param EventContext eventContext);

	@Message(id = ID_OFFSET + 81, value = "The value '%1$s' cannot be indexed because its absolute value is too large.")
	SearchException scaledNumberTooLarge(Number value);

	@Message(id = ID_OFFSET + 82,
			value = "Invalid index field type: decimal scale '%1$s' is positive."
						+ " The decimal scale of BigInteger fields must be zero or negative.")
	SearchException invalidDecimalScale(Integer decimalScale, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 84,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from a scope targeting indexes %3$s,"
					+ " but the given predicate was built from a scope targeting indexes %2$s.")
	SearchException predicateDefinedOnDifferentIndexes(SearchPredicate predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 85,
			value = "Invalid search sort: '%1$s'. You must build the sort from a scope targeting indexes %3$s,"
					+ " but the given sort was built from a scope targeting indexes %2$s.")
	SearchException sortDefinedOnDifferentIndexes(SearchSort predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 86,
			value = "Invalid search projection: '%1$s'. You must build the projection from a scope targeting indexes %3$s,"
					+ " but the given projection was built from a scope targeting indexes %2$s.")
	SearchException projectionDefinedOnDifferentIndexes(SearchProjection<?> predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 87,
			value = "Invalid filesystem access strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidFileSystemAccessStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 88,
			value = "Invalid locking strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidLockingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 89,
			value = "Incorrect sharding strategy implementation:"
					+ " strategy '%1$s' did not declare any shard identifiers during initialization."
					+ " Declare shard identifiers using context.shardIdentifiers(...) or,"
					+ " if sharding is disabled, call context.disableSharding().")
	SearchException missingShardIdentifiersAfterShardingStrategyInitialization(Object strategy);

	@Message(id = ID_OFFSET + 90,
			value = "Invalid configuration for sharding strategy '%1$s': configuration property '%2$s' must be set.")
	SearchException missingPropertyValueForShardingStrategy(String strategyName, String propertyKey);

	@Message(id = ID_OFFSET + 91,
			value = "Invalid routing key: '%1$s'. Valid keys are: %2$s.")
	SearchException invalidRoutingKeyForExplicitShardingStrategy(String invalidKey, Collection<String> validKeys);

	@Message(id = ID_OFFSET + 94,
			value = "Invalid index field type: both analyzer '%1$s' and aggregations are enabled."
					+ " Aggregations are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not aggregable, and one with a normalizer that is aggregable.")
	SearchException cannotUseAnalyzerOnAggregableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 98,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherAggregations(SearchAggregation<?> aggregation);

	@Message(id = ID_OFFSET + 99,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from a scope targeting indexes %3$s,"
					+ " but the given aggregation was built from a scope targeting indexes %2$s.")
	SearchException aggregationDefinedOnDifferentIndexes(SearchAggregation<?> aggregation,
			Set<String> aggregationIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 102,
			value = "Duplicate aggregation definitions for key: '%1$s'")
	SearchException duplicateAggregationKey(@FormatWith(AggregationKeyFormatter.class) AggregationKey<?> key);

	@Message(id = ID_OFFSET + 104,
			value = "Invalid index field type: search analyzer '%1$s' is assigned to this type,"
				+ " but the indexing analyzer is missing."
				+ " Assign an indexing analyzer and a search analyzer, or remove the search analyzer.")
	SearchException searchAnalyzerWithoutAnalyzer(String searchAnalyzer, @Param EventContext context);

	@Message(id = ID_OFFSET + 105,
			value = "Multiple conflicting models for object field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingObjectFieldModel(String absoluteFieldPath,
			LuceneSearchObjectFieldContext index1Model, LuceneSearchObjectFieldContext index2Model, @Param EventContext context);

	@Message(id = ID_OFFSET + 106,
			value = "Multiple conflicting models for field '%1$s': '%2$s' vs. '%3$s'.")
	SearchException conflictingFieldModel(String absoluteFieldPath,
			AbstractLuceneIndexSchemaFieldNode fieldNode1, AbstractLuceneIndexSchemaFieldNode fieldNode2,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 108,
			value = "Invalid I/O strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidIOStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 109,
			value = "Index does not exist for directory '%1$s'")
	SearchException missingIndex(Directory directory, @Param EventContext context);

	@Message(id = ID_OFFSET + 110,
			value = "Unable to validate index directory: %1$s")
	SearchException unableToValidateIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 111,
			value = "Unable to drop index directory: %1$s")
	SearchException unableToDropIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 113,
			value = "Invalid target fields for simple-query-string predicate:"
					+ " fields [%1$s, %3$s] are in different nested documents [%2$s, %4$s]."
					+ " All fields targeted by a simple-query-string predicate must be in the same document.")
	SearchException simpleQueryStringSpanningMultipleNestedPaths(String fieldPath1, String nestedPath1,
			String fieldPath2, String nestedPath2);

	@Message(id = ID_OFFSET + 114,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for fields in nested documents.")
	SearchException invalidSortModeAcrossNested(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 115,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for String fields."
					+ " Only MIN and MAX are supported.")
	SearchException invalidSortModeForStringField(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 116,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for temporal fields."
					+ " Only MIN, MAX, AVG and MEDIAN are supported.")
	SearchException invalidSortModeForTemporalField(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 117,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for a distance sort."
					+ " Only MIN, MAX, AVG and MEDIAN are supported.")
	SearchException invalidSortModeForDistanceSort(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 118,
			value = "A failure occurred during a low-level write operation"
					+ " and the index writer had to be reset."
					+ " Some write operations may have been lost as a result."
					+ " Failure: %1$s")
	SearchException uncommittedOperationsBecauseOfFailure(String causeMessage,
			@Param EventContext context, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 120,
			value = "Invalid sort filter: field '%1$s' is not contained in a nested object."
					+ " Sort filters are only available if the field to sort on is contained in a nested object.")
	SearchException cannotFilterSortOnRootDocumentField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 121,
			value = "Invalid search predicate: %1$s. This predicate targets fields %3$s,"
					+ " but only fields that are contained in the nested object with path '%2$s' are allowed here.")
	SearchException invalidNestedObjectPathForPredicate(SearchPredicate predicate, String nestedObjectPath,
			List<String> fieldPaths);

	@Message(id = ID_OFFSET + 122,
			value = "Invalid aggregation filter: field '%1$s' is not contained in a nested object."
					+ " Aggregation filters are only available if the field to aggregate on is contained in a nested object.")
	SearchException cannotFilterAggregationOnRootDocumentField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 123,
			value = "Invalid value for IndexWriter setting '%1$s': '%2$s'. %3$s")
	SearchException illegalIndexWriterSetting(String settingName, Object settingValue, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 124,
			value = "Invalid value for merge policy setting '%1$s': '%2$s'. %3$s")
	SearchException illegalMergePolicySetting(String settingName, Object settingValue, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 125,
			value = "Duplicate index field template definition: '%1$s'."
					+ " Multiple bridges may be trying to access the same index field template, "
					+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.")
	SearchException indexSchemaFieldTemplateNameConflict(String name, @Param EventContext context);

	@Message(id = ID_OFFSET + 126,
			value = "Invalid value type. This field's values are of type '%1$s', which is not assignable from '%2$s'.")
	SearchException invalidFieldValueType(@FormatWith(ClassFormatter.class) Class<?> fieldValueType,
			@FormatWith(ClassFormatter.class) Class<?> invalidValueType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 127,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForIndexing(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 128,
			value = "Invalid type: the index root is not an object field.")
	SearchException invalidIndexElementTypeRootIsNotObjectField();

	@Message(id = ID_OFFSET + 129,
			value = "Invalid type: '%1$s' is a value field, not an object field.")
	SearchException invalidIndexElementTypeValueFieldIsNotObjectField(String absolutePath);

	@Message(id = ID_OFFSET + 130,
			value = "Invalid type: '%1$s' is an object field, not a value field.")
	SearchException invalidIndexElementTypeObjectFieldIsNotValueField(String absolutePath);

	@Message(id = ID_OFFSET + 131,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is multi-valued."
					+ " Make sure to call '.multi()' when you create the projection.")
	SearchException invalidSingleValuedProjectionOnMultiValuedField(String absolutePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 132, value = "Cannot use '%2$s' on field '%1$s'."
			+ " Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant)."
			+ " If it already is, then '%2$s' is not available for fields of this type.")
	SearchException cannotUseQueryElementForField(String absoluteFieldPath, String queryElementName, @Param EventContext context);

	@Message(id = ID_OFFSET + 133,
			value = "Inconsistent support for '%1$s': %2$s")
	SearchException inconsistentSupportForQueryElement(String queryElementName,
			String causeMessage, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 134,
			value = "Field attribute '%1$s' differs: '%2$s' vs. '%3$s'.")
	SearchException differentFieldAttribute(String attributeName, Object component1, Object component2);

	@Message(id = ID_OFFSET + 135,
			value = "Implementation class differs: '%1$s' vs. '%2$s'.")
	SearchException differentImplementationClassForQueryElement(@FormatWith(ClassFormatter.class) Class<?> class1,
			@FormatWith(ClassFormatter.class) Class<?> class2);

	@Message(id = ID_OFFSET + 136,
			value = "Field codec differs: '%1$s' vs. '%2$s'.")
	SearchException differentFieldCodecForQueryElement(Object codec1, Object codec2);

	@Message(id = ID_OFFSET + 137,
			value = "'%1$s' can be used in some of the targeted indexes, but not all of them."
					+ " Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant) in all indexes,"
					+ " and that the field has the same type in all indexes.")
	SearchException partialSupportForQueryElement(String queryElementName);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 138, value = "Using deprecated filesystem access strategy '%1$s',"
			+ " which will be removed in a future version of Lucene."
			+ " %2$s")
	void deprecatedFileSystemAccessStrategy(String accessStrategyName,
			@FormatWith(EventContextFormatter.class) EventContext eventContext);

	@Message(id = ID_OFFSET + 139, value = "Cannot use '%2$s' on field '%1$s'."
			+ " '%2$s' is not available for object fields.")
	SearchException cannotUseQueryElementForObjectField(String absoluteFieldPath, String queryElementName, @Param EventContext context);

	@Message(id = ID_OFFSET + 140, value = "Cannot use '%2$s' on field '%1$s': %3$s")
	SearchException cannotUseQueryElementForObjectFieldBecauseCreationException(String absoluteFieldPath,
			String queryElementName, String causeMessage, @Cause SearchException cause, @Param EventContext context);

	@Message(id = ID_OFFSET + 141,
			value = "Unable to compute size of index: %1$s")
	SearchException unableToComputeIndexSize(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 142,
			value = "Unable to create instance of analysis component '%1$s': %2$s")
	SearchException unableToCreateAnalysisComponent(@FormatWith(ClassFormatter.class) Class<?> type, String causeMessage,
			@Cause Exception cause);
}
