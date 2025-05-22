/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET;
import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET_LEGACY_ENGINE;
import static org.jboss.logging.Logger.Level.TRACE;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
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
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import org.apache.lucene.search.Query;

@CategorizedLogger(
		category = QueryLog.CATEGORY_NAME,
		description = """
				Logs the Lucene queries that are about to be executed and other query related messages.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface QueryLog {
	String CATEGORY_NAME = "org.hibernate.search.query.lucene";

	QueryLog INSTANCE = LoggerFactory.make( QueryLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 274, value = "Executing Lucene query: %s")
	void executingLuceneQuery(Query luceneQuery);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 10,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = ID_OFFSET + 14,
			value = "Invalid search sort: '%1$s'. You must build the sort from a Lucene search scope.")
	SearchException cannotMixLuceneSearchSortWithOtherSorts(SearchSort sort);

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

	@Message(id = ID_OFFSET + 44,
			value = "Computed minimum for minimumShouldMatch constraint is out of bounds:"
					+ " expected a number between '1' and '%1$s', got '%2$s'.")
	SearchException minimumShouldMatchMinimumOutOfBounds(int totalShouldClauseNumber, int minimum);

	@Message(id = ID_OFFSET + 45,
			value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ignoreConstraintCeiling);

	@Message(id = ID_OFFSET + 55,
			value = "Invalid search projection: '%1$s'. You must build the projection from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherProjections(SearchProjection<?> projection);

	@Message(id = ID_OFFSET + 67, value = "Unable to delete all entries matching query '%1$s': %2$s")
	SearchException unableToDeleteAllEntriesFromIndex(Query query, String causeMessage, @Param EventContext context,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 70,
			value = "Full-text features (analysis, fuzziness, minimum should match) are not supported for fields of this type.")
	SearchException fullTextFeaturesNotSupportedByFieldType(@Param EventContext context);

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

	@Message(id = ID_OFFSET + 84,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given predicate was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException predicateDefinedOnDifferentIndexes(SearchPredicate predicate, Set<String> predicateIndexes,
			Set<String> scopeIndexes, Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 85,
			value = "Invalid search sort: '%1$s'. You must build the sort from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given sort was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException sortDefinedOnDifferentIndexes(SearchSort sort, Set<String> sortIndexes, Set<String> scopeIndexes,
			Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 86,
			value = "Invalid search projection: '%1$s'. You must build the projection from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given projection was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException projectionDefinedOnDifferentIndexes(SearchProjection<?> projection, Set<String> projectionIndexes,
			Set<String> scopeIndexes, Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 98,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherAggregations(SearchAggregation<?> aggregation);

	@Message(id = ID_OFFSET + 99,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given aggregation was built from a scope targeting indexes %2$s, where indexes %4$s are missing")
	SearchException aggregationDefinedOnDifferentIndexes(SearchAggregation<?> aggregation,
			Set<String> aggregationIndexes, Set<String> scopeIndexes, Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 102,
			value = "Duplicate aggregation definitions for key: '%1$s'")
	SearchException duplicateAggregationKey(@FormatWith(AggregationKeyFormatter.class) AggregationKey<?> key);

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

	@Message(id = ID_OFFSET + 131,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is multi-valued."
					+ " Make sure to call '.collector(...)' when you create the projection.")
	SearchException invalidSingleValuedProjectionOnMultiValuedField(String absolutePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 135,
			value = "Implementation class differs: '%1$s' vs. '%2$s'.")
	SearchException differentImplementationClassForQueryElement(@FormatWith(ClassFormatter.class) Class<?> class1,
			@FormatWith(ClassFormatter.class) Class<?> class2);

	@Message(id = ID_OFFSET + 136,
			value = "Field codec differs: '%1$s' vs. '%2$s'.")
	SearchException differentFieldCodecForQueryElement(Object codec1, Object codec2);

	@Message(id = ID_OFFSET + 144,
			value = "Predicate definition differs: '%1$s' vs. '%2$s'.")
	SearchException differentPredicateDefinitionForQueryElement(Object predicateDefinition1, Object predicateDefinition2);

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
	SearchException invalidContextForProjectionOnField(String absolutePath, String objectFieldAbsolutePath);

	@Message(id = ID_OFFSET + 153,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is effectively multi-valued in this context,"
					+ " because parent object field '%2$s' is multi-valued."
					+ " Either call '.collector(...)' when you create the projection on field '%1$s',"
					+ " or wrap that projection in an object projection like this:"
					+ " 'f.object(\"%2$s\").from(<the projection on field %1$s>).as(...).collector(...)'.")
	SearchException invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(String absolutePath,
			String objectFieldAbsolutePath);

	@Message(id = ID_OFFSET + 155,
			value = "Unexpected mapped type name extracted from hits: '%1$s'. Expected one of: %2$s."
					+ " The document was probably indexed with a different configuration: full reindexing is necessary.")
	SearchException unexpectedMappedTypeNameForByMappedTypeProjection(String typeName, Set<String> expectedTypeNames);

	@Message(id = ID_OFFSET + 158,
			value = "Invalid highlighter: '%1$s'. You must build the highlighter from a Lucene search scope.")
	SearchException cannotMixLuceneSearchQueryWithOtherQueryHighlighters(SearchHighlighter highlighter);

	@Message(id = ID_OFFSET + 159,
			value = "Invalid highlighter: '%1$s'. You must build the highlighter from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given highlighter was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException queryHighlighterDefinedOnDifferentIndexes(SearchHighlighter highlighter, Set<String> highlighterIndexes,
			Set<String> scopeIndexes, Set<String> scopeDifference);

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

	@LogMessage(level = Logger.Level.WARN)
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

	@Message(id = ID_OFFSET + 117,
			value = "Invalid sort mode: %1$s. This sort mode is not supported for a distance sort."
					+ " Only MIN, MAX, AVG and MEDIAN are supported.")
	SearchException invalidSortModeForDistanceSort(SortMode mode, @Param EventContext context);

	@Message(id = ID_OFFSET + 176, value = "Vector field '%1$s' is defined as a '%2$s' array."
			+ " Matching against '%3$s' array is unsupported."
			+ " Use the array of the same type as the vector field.")
	SearchException vectorKnnMatchVectorTypeDiffersFromField(String absoluteFieldPath,
			@FormatWith(ClassFormatter.class) Class<?> expected, @FormatWith(ClassFormatter.class) Class<?> actual);

	@Message(id = ID_OFFSET + 178, value = "Vector field '%1$s' is defined as a vector with '%2$s' dimensions (array length)."
			+ " Matching against an array with length of '%3$s' is unsupported."
			+ " Use the array of the same size as the vector field.")
	SearchException vectorKnnMatchVectorDimensionDiffersFromField(String absoluteFieldPath, int expected, int actual);

	@Message(id = ID_OFFSET + 180, value = "An error occurred while parsing the query string '%1$s': %2$s")
	SearchException queryStringParseException(String query, String message, @Cause Exception cause);

	@Message(id = ID_OFFSET + 186, value = "A single-valued highlight projection requested, "
			+ "but the corresponding highlighter does not set number of fragments to 1.")
	SearchException highlighterIncompatibleCardinality();

	@Message(id = ID_OFFSET + 194, value = "Current factory cannot be resocped to '%1$s' as it is scoped to '%2$s'.")
	SearchException incompatibleScopeRootType(@FormatWith(ClassFormatter.class) Class<?> requested,
			@FormatWith(ClassFormatter.class) Class<?> actual);
}
