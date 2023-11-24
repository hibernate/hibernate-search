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

import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.logging.spi.AggregationKeyFormatter;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
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

import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

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
	void recommendConfiguringLuceneVersion(String key, Version latest,
			@FormatWith(EventContextFormatter.class) EventContext context);

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
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 226, value = "%s: %s")
	void logInfoStreamMessage(String componentName, String message);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 228,
			value = "Unable to parse '%1$ss' into a Lucene version: %2$s")
	SearchException illegalLuceneVersionFormat(String property, String luceneErrorMessage, @Cause Exception e);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 274, value = "Executing Lucene query: %s")
	void executingLuceneQuery(Query luceneQuery);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 284,
			value = "Unable to open index readers: %1$s")
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

	@Message(id = ID_OFFSET + 14,
			value = "Invalid search sort: '%1$s'. You must build the sort from a Lucene search scope.")
	SearchException cannotMixLuceneSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = ID_OFFSET + 15,
			value = "Unable to initialize index directory: %1$s")
	SearchException unableToInitializeIndexDirectory(String causeMessage,
			@Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 16,
			value = "Unable to index entity of type '%2$s' with identifier '%3$s' and tenant identifier '%1$s': %4$s")
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

	@Message(id = ID_OFFSET + 30,
			value = "Invalid multi-tenancy strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 31,
			value = "Invalid tenant identifiers: '%1$s'."
					+ " No tenant identifier is expected, because multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(Set<String> tenantId, @Param EventContext context);

	@Message(id = ID_OFFSET + 32,
			value = "Missing tenant identifier."
					+ " A tenant identifier is expected, because multi-tenancy is enabled for this backend.")
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

	@Message(id = ID_OFFSET + 61, value = "Unable to shut down index: %1$s")
	SearchException unableToShutdownShard(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 62, value = "No built-in index field type for class: '%1$s'.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET + 67, value = "Unable to delete all entries matching query '%1$s': %2$s")
	SearchException unableToDeleteAllEntriesFromIndex(Query query, String causeMessage, @Param EventContext context,
			@Cause Exception cause);

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
					+ " Use explain(String typeName, Object id) and pass one of %1$s as the type name.")
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
			+ " Define the decimal scale explicitly. %1$s")
	SearchException nullDecimalScale(String hint, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 81,
			value = "Unable to encode value '%1$s': this field type only supports values ranging from '%2$s' to '%3$s'."
					+ " If you want to encode values that are outside this range, change the decimal scale for this field."
					+ " Do not forget to reindex all your data after changing the decimal scale.")
	SearchException scaledNumberTooLarge(Number value, Number min, Number max);

	@Message(id = ID_OFFSET + 82,
			value = "Invalid index field type: decimal scale '%1$s' is positive."
					+ " The decimal scale of BigInteger fields must be zero or negative.")
	SearchException invalidDecimalScale(Integer decimalScale, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 84,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from a scope targeting indexes %3$s,"
					+ " but the given predicate was built from a scope targeting indexes %2$s.")
	SearchException predicateDefinedOnDifferentIndexes(SearchPredicate predicate, Set<String> predicateIndexes,
			Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 85,
			value = "Invalid search sort: '%1$s'. You must build the sort from a scope targeting indexes %3$s,"
					+ " but the given sort was built from a scope targeting indexes %2$s.")
	SearchException sortDefinedOnDifferentIndexes(SearchSort predicate, Set<String> predicateIndexes, Set<String> scopeIndexes);

	@Message(id = ID_OFFSET + 86,
			value = "Invalid search projection: '%1$s'. You must build the projection from a scope targeting indexes %3$s,"
					+ " but the given projection was built from a scope targeting indexes %2$s.")
	SearchException projectionDefinedOnDifferentIndexes(SearchProjection<?> predicate, Set<String> predicateIndexes,
			Set<String> scopeIndexes);

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
			value = "When using sharding strategy '%1$s', this configuration property must be set.")
	SearchException missingPropertyValueForShardingStrategy(String strategyName);

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

	@Message(id = ID_OFFSET + 131,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is multi-valued."
					+ " Make sure to call '.multi()' when you create the projection.")
	SearchException invalidSingleValuedProjectionOnMultiValuedField(String absolutePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 135,
			value = "Implementation class differs: '%1$s' vs. '%2$s'.")
	SearchException differentImplementationClassForQueryElement(@FormatWith(ClassFormatter.class) Class<?> class1,
			@FormatWith(ClassFormatter.class) Class<?> class2);

	@Message(id = ID_OFFSET + 136,
			value = "Field codec differs: '%1$s' vs. '%2$s'.")
	SearchException differentFieldCodecForQueryElement(Object codec1, Object codec2);

	@Message(id = ID_OFFSET + 141,
			value = "Unable to compute size of index: %1$s")
	SearchException unableToComputeIndexSize(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 142,
			value = "Unable to create instance of analysis component '%1$s': %2$s")
	SearchException unableToCreateAnalysisComponent(@FormatWith(ClassFormatter.class) Class<?> type, String causeMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 143,
			value = "The index schema named predicate '%1$s' was added twice.")
	SearchException indexSchemaNamedPredicateNameConflict(String relativeFilterName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 144,
			value = "Predicate definition differs: '%1$s' vs. '%2$s'.")
	SearchException differentPredicateDefinitionForQueryElement(Object predicateDefinition1, Object predicateDefinition2);

	@Message(id = ID_OFFSET + 146,
			value = "Unable to apply query caching configuration: %1$s")
	SearchException unableToApplyQueryCacheConfiguration(String errorMessage, @Cause Exception e);

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

	@Message(id = ID_OFFSET + 151,
			value = "Offset + limit should be lower than Integer.MAX_VALUE, offset: '%1$s', limit: '%2$s'.")
	IOException offsetLimitExceedsMaxValue(int offset, Integer limit);

	@Message(id = ID_OFFSET + 152,
			value = "Invalid context for projection on field '%1$s': the surrounding projection"
					+ " is executed for each object in field '%2$s', which is not a parent of field '%1$s'."
					+ " Check the structure of your projections.")
	SearchException invalidContextForProjectionOnField(String absolutePath,
			String objectFieldAbsolutePath);

	@Message(id = ID_OFFSET + 153,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is effectively multi-valued in this context,"
					+ " because parent object field '%2$s' is multi-valued."
					+ " Either call '.multi()' when you create the projection on field '%1$s',"
					+ " or wrap that projection in an object projection like this:"
					+ " 'f.object(\"%2$s\").from(<the projection on field %1$s>).as(...).multi()'.")
	SearchException invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(String absolutePath,
			String objectFieldAbsolutePath);

	@Message(id = ID_OFFSET + 154,
			value = "Unable to start index: %1$s")
	SearchException unableToStartShard(String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 155,
			value = "Unexpected mapped type name extracted from hits: '%1$s'. Expected one of: %2$s."
					+ " The document was probably indexed with a different configuration: full reindexing is necessary.")
	SearchException unexpectedMappedTypeNameForByMappedTypeProjection(String typeName, Set<String> expectedTypeNames);

	@Message(value = "This multi-valued field has a 'FLATTENED' structure,"
			+ " which means the structure of objects is not preserved upon indexing,"
			+ " making object projections impossible."
			+ " Try setting the field structure to 'NESTED' and reindexing all your data.")
	String missingSupportHintForObjectProjectionOnMultiValuedFlattenedObjectNode();

	@Message(id = ID_OFFSET + 156, value = "Nonblocking operation submitter is not supported.")
	SearchException nonblockingOperationSubmitterNotSupported();

	@Message(id = ID_OFFSET + 157, value = "Unable to export the schema for '%1$s' index: %2$s")
	SearchException unableToExportSchema(String indexName, String message, @Cause Exception cause);

	@Message(id = ID_OFFSET + 158,
			value = "Invalid highlighter: '%1$s'. You must build the highlighter from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherQueryHighlighters(SearchHighlighter highlighter);

	@Message(id = ID_OFFSET + 159,
			value = "Invalid highlighter: '%1$s'. You must build the highlighter from a scope targeting indexes %3$s,"
					+ " but the given highlighter was built from a scope targeting indexes %2$s.")
	SearchException queryHighlighterDefinedOnDifferentIndexes(SearchHighlighter highlighter, Set<String> indexNames,
			Set<String> hibernateSearchIndexNames);

	@Message(id = ID_OFFSET + 160,
			value = "Overriding a '%2$s' highlighter with a '%1$s' is not supported. " +
					"Overriding highlighters should be of the same type as the global is if the global highlighter was configured.")
	SearchException cannotMixDifferentHighlighterTypesAtOverrideLevel(SearchHighlighterType override,
			SearchHighlighterType parent);

	@Message(id = ID_OFFSET + 161,
			value = "Cannot find a highlighter with name '%1$s'." +
					" Available highlighters are: %2$s." +
					" Was it configured with `highlighter(\"%1$s\", highlighterContributor)`?")
	SearchException cannotFindHighlighterWithName(String name, Collection<String> availableHighlighterNames);

	@Message(id = ID_OFFSET + 162,
			value = "'%1$s' highlighter does not support '%2$s' boundary scanner type.")
	SearchException unsupportedBoundaryScannerType(String type, BoundaryScannerType boundaryScannerType);

	@Message(id = ID_OFFSET + 163, value = "Named highlighters cannot use a blank string as name.")
	SearchException highlighterNameCannotBeBlank();

	@Message(id = ID_OFFSET + 164,
			value = "Highlighter with name '%1$s' is already defined. Use a different name to add another highlighter.")
	SearchException highlighterWithTheSameNameCannotBeAdded(String highlighterName);

	@Message(id = ID_OFFSET + 165,
			value = "'%1$s' highlighter type cannot be applied to '%2$s' field. " +
					"'%2$s' must have either 'ANY' or '%1$s' among the configured highlightable values.")
	SearchException highlighterTypeNotSupported(SearchHighlighterType type, String field);

	@Message(id = ID_OFFSET + 166,
			value = "Cannot use 'NO' in combination with other highlightable values. Applied values are: '%1$s'")
	SearchException unsupportedMixOfHighlightableValues(Set<Highlightable> highlightable);

	@Message(id = ID_OFFSET + 167,
			value = "The '%1$s' term vector storage strategy is not compatible with the fast vector highlighter. " +
					"Either change the strategy to one of `WITH_POSITIONS_PAYLOADS`/`WITH_POSITIONS_OFFSETS_PAYLOADS` or remove the requirement for the fast vector highlighter support.")
	SearchException termVectorDontAllowFastVectorHighlighter(TermVector termVector);

	@Message(id = ID_OFFSET + 168,
			value = "Setting the `highlightable` attribute to an empty array is not supported. " +
					"Set the value to `NO` if the field does not require the highlight projection.")
	SearchException noHighlightableProvided();

	@LogMessage(level = Level.WARN)
	@Message(id = ID_OFFSET + 169,
			value = "Lucene's unified highlighter cannot limit the size of a fragment returned when no match is found. " +
					"Instead if no match size was set to any positive integer - all text will be returned. " +
					"Configured value '%1$s' will be ignored, and the fragment will not be limited. " +
					"If you don't want to see this warning set the value to Integer.MAX_VALUE.")
	void unifiedHighlighterNoMatchSizeWarning(Integer value);

	@Message(id = ID_OFFSET + 170,
			value = "Lucene's unified highlighter does not support the size fragment setting. " +
					"Either use a plain or fast vector highlighters, or do not set this setting.")
	SearchException unifiedHighlighterFragmentSizeNotSupported();

	@Message(id = ID_OFFSET + 171,
			value = "Highlight projection cannot be applied within nested context of '%1$s'.")
	SearchException cannotHighlightInNestedContext(String currentNestingField,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET + 172,
			value = "The highlight projection cannot be applied to a field from an object using `ObjectStructure.NESTED` structure.")
	SearchException cannotHighlightFieldFromNestedObjectStructure(@Param EventContext eventContext);

	@Message(id = ID_OFFSET + 173, value = "'%1$s' cannot be nested in an object projection. "
			+ "%2$s")
	SearchException cannotUseProjectionInNestedContext(String projection, String hint, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 174,
			value = "Vector '%1$s' cannot be equal to '%2$s'. It must be a positive integer value lesser than or equal to %3$s.")
	SearchException vectorPropertyUnsupportedValue(String property, Integer value, int max);

	@Message(id = ID_OFFSET + 175, value = "No built-in vector index field type for class: '%1$s'.")
	SearchException cannotGuessVectorFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 176, value = "Vector field '%1$s' is defined as a '%2$s' array."
			+ " Matching against '%3$s' array is unsupported."
			+ " Use the array of the same type as the vector field.")
	SearchException vectorKnnMatchVectorTypeDiffersFromField(String absoluteFieldPath,
			@FormatWith(ClassFormatter.class) Class<?> expected, @FormatWith(ClassFormatter.class) Class<?> actual);

	@Message(id = ID_OFFSET + 177, value = "Fields of this type cannot be multivalued.")
	SearchException multiValuedFieldNotAllowed(@Param EventContext context);
}
