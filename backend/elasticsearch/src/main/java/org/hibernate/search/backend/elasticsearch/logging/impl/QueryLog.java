/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;
import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET_LEGACY_ES;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.logging.spi.AggregationKeyFormatter;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = QueryLog.CATEGORY_NAME,
		description = """
				Logs the Elasticsearch queries that are about to be executed and other query related messages.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface QueryLog {
	String CATEGORY_NAME = "org.hibernate.search.query.elasticsearch";

	QueryLog INSTANCE = LoggerFactory.make( QueryLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET_LEGACY_ES + 53,
			value = "Executing Elasticsearch query on '%s' with parameters '%s': <%s>")
	void executingElasticsearchQuery(String path, Map<String, String> parameters,
			String bodyParts);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 2,
			value = "Invalid multi-index scope: a scope cannot span both a Elasticsearch index and another type of index."
					+ " Base scope: '%1$s', incompatible (Elasticsearch) index: '%2$s'.")
	SearchException cannotMixElasticsearchScopeWithOtherType(IndexScopeBuilder<?> baseScope,
			ElasticsearchIndexManager elasticsearchIndex, @Param EventContext context);

	@Message(id = ID_OFFSET + 3,
			value = "Invalid multi-index scope: a scope cannot span multiple Elasticsearch backends."
					+ " Base scope: '%1$s', incompatible index (from another backend): '%2$s'.")
	SearchException cannotMixElasticsearchScopeWithOtherBackend(IndexScopeBuilder<?> baseScope,
			ElasticsearchIndexManager indexFromOtherBackend, @Param EventContext context);

	@Message(id = ID_OFFSET + 8,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = ID_OFFSET + 11,
			value = "Invalid search sort: '%1$s'. You must build the sort from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = ID_OFFSET + 29,
			value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ceiling);

	@Message(id = ID_OFFSET + 38,
			value = "Invalid search projection: '%1$s'. You must build the projection from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherProjections(SearchProjection<?> projection);

	@Message(id = ID_OFFSET + 53,
			value = "Full-text features (analysis, fuzziness, minimum should match) are not supported for fields of this type.")
	SearchException fullTextFeaturesNotSupportedByFieldType(@Param EventContext context);

	@Message(id = ID_OFFSET + 64,
			value = "Invalid use of explain(Object id) on a query targeting multiple types."
					+ " Use explain(String typeName, Object id) and pass one of %1$s as the type name.")
	SearchException explainRequiresTypeName(Set<String> targetedTypeNames);

	@Message(id = ID_OFFSET + 65,
			value = "Invalid mapped type name: '%2$s'."
					+ " This type is not among the mapped types targeted by this query: %1$s.")
	SearchException explainRequiresTypeTargetedByQuery(Set<String> targetedTypeNames, String typeName);

	@Message(id = ID_OFFSET + 66,
			value = "Invalid document identifier: '%2$s'. No such document in index '%1$s'.")
	SearchException explainUnknownDocument(URLEncodedString indexName, URLEncodedString id);

	@Message(id = ID_OFFSET + 72,
			value = "Invalid search predicate: '%1$s'. You must build the predicate from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given predicate was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException predicateDefinedOnDifferentIndexes(SearchPredicate predicate, Set<String> predicateIndexes,
			Set<String> scopeIndexes, Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 73,
			value = "Invalid search sort: '%1$s'. You must build the sort from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given sort was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException sortDefinedOnDifferentIndexes(SearchSort sort, Set<String> sortIndexes, Set<String> scopeIndexes,
			Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 74,
			value = "Invalid search projection: '%1$s'. You must build the projection from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given projection was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException projectionDefinedOnDifferentIndexes(SearchProjection<?> projection, Set<String> projectionIndexes,
			Set<String> scopeIndexes, Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 80,
			value = "Invalid range: '%1$s'. Elasticsearch range aggregations only accept ranges in the canonical form:"
					+ " (-Infinity, <value>) or [<value1>, <value2>) or [<value>, +Infinity)."
					+ " Call Range.canonical(...) to be sure to create such a range.")
	SearchException elasticsearchRangeAggregationRequiresCanonicalFormForRanges(Range<?> range);

	@Message(id = ID_OFFSET + 81,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from an Elasticsearch search scope.")
	SearchException cannotMixElasticsearchSearchQueryWithOtherAggregations(SearchAggregation<?> aggregation);

	@Message(id = ID_OFFSET + 82,
			value = "Invalid search aggregation: '%1$s'. You must build the aggregation from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given aggregation was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException aggregationDefinedOnDifferentIndexes(SearchAggregation<?> aggregation,
			Set<String> aggregationIndexes, Set<String> scopeIndexes, Set<String> scopeDifference);

	@Message(id = ID_OFFSET + 85,
			value = "Duplicate aggregation definitions for key: '%1$s'")
	SearchException duplicateAggregationKey(@FormatWith(AggregationKeyFormatter.class) AggregationKey<?> key);

	@Message(id = ID_OFFSET + 92,
			value = "Missing field '%1$s' for one of the search hits."
					+ " The document was probably indexed with a different configuration: full reindexing is necessary.")
	SearchException missingTypeFieldInDocument(String fieldName);

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

	@Message(id = ID_OFFSET + 113,
			value = "Invalid cardinality for projection on field '%1$s': the projection is single-valued,"
					+ " but this field is multi-valued."
					+ " Make sure to call '.collector(...)' when you create the projection.")
	SearchException invalidSingleValuedProjectionOnMultiValuedField(String absolutePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 117,
			value = "Implementation class differs: '%1$s' vs. '%2$s'.")
	SearchException differentImplementationClassForQueryElement(@FormatWith(ClassFormatter.class) Class<?> class1,
			@FormatWith(ClassFormatter.class) Class<?> class2);

	@Message(id = ID_OFFSET + 118,
			value = "Field codec differs: '%1$s' vs. '%2$s'.")
	SearchException differentFieldCodecForQueryElement(Object codec1, Object codec2);

	@Message(id = ID_OFFSET + 134,
			value = "Invalid use of 'missing().first()' for an ascending distance sort. Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized.")
	SearchException missingFirstOnAscSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 135,
			value = "Invalid use of 'missing().last()' for a descending distance sort. Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized.")
	SearchException missingLastOnDescSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 136,
			value = "Invalid use of 'missing().use(...)' for a distance sort. Elasticsearch always assumes missing values have a distance of '+Infinity', and this behavior cannot be customized.")
	SearchException missingAsOnSortNotSupported(@Param EventContext context);

	@Message(id = ID_OFFSET + 138,
			value = "Predicate definition differs: '%1$s' vs. '%2$s'.")
	SearchException differentPredicateDefinitionForQueryElement(Object predicateDefinition1, Object predicateDefinition2);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 140, value = "A search query fetching all hits was requested," +
			" but only '%2$s' hits were retrieved because the maximum result window size forces a limit of '%1$s'" +
			" hits. Refer to Elasticsearch's 'max_result_window_size' setting for more information.")
	void defaultedLimitedHits(Integer defaultLimit, long hitCount);

	@Message(id = ID_OFFSET + 150,
			value = "Param with name '%1$s' has not been defined for the named predicate '%2$s'.")
	SearchException paramNotDefined(String name, String predicateName, @Param EventContext context);

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
					+ " Either call '.collector(...)' when you create the projection on field '%1$s',"
					+ " or wrap that projection in an object projection like this:"
					+ " 'f.object(\"%2$s\").from(<the projection on field %1$s>).as(...).collector(...)'.")
	SearchException invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(String absolutePath,
			String objectFieldAbsolutePath);

	@Message(id = ID_OFFSET + 156,
			value = "Unexpected mapped type name extracted from hits: '%1$s'. Expected one of: %2$s."
					+ " The document was probably indexed with a different configuration: full reindexing is necessary.")
	SearchException unexpectedMappedTypeNameForByMappedTypeProjection(String typeName, Set<String> expectedTypeNames);

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
			value = "Invalid highlighter: '%1$s'. You must build the highlighter from a scope targeting indexes %3$s or a superset of them,"
					+ " but the given highlighter was built from a scope targeting indexes %2$s, where indexes %4$s are missing.")
	SearchException queryHighlighterDefinedOnDifferentIndexes(SearchHighlighter highlighter, Set<String> highlighterIndexes,
			Set<String> scopeIndexes, Set<String> scopeDifference);

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

	@Message(id = ID_OFFSET + 181, value = "Vector field '%1$s' is defined as a '%2$s' array."
			+ " Matching against '%3$s' array is unsupported."
			+ " Use the array of the same type as the vector field.")
	SearchException vectorKnnMatchVectorTypeDiffersFromField(String absoluteFieldPath,
			@FormatWith(ClassFormatter.class) Class<?> expected, @FormatWith(ClassFormatter.class) Class<?> actual);

	@Message(id = ID_OFFSET + 182, value = "Vector field '%1$s' is defined as a vector with '%2$s' dimensions (array length)."
			+ " Matching against an array with length of '%3$s' is unsupported."
			+ " Use the array of the same size as the vector field.")
	SearchException vectorKnnMatchVectorDimensionDiffersFromField(String absoluteFieldPath, int expected, int actual);

	@Message(id = ID_OFFSET + 187,
			value = "An OpenSearch distribution does not allow specifying the `required minimum similarity` option. "
					+ "This option is only applicable to an Elastic distribution of an Elasticsearch backend.")
	SearchException knnRequiredMinimumSimilarityUnsupportedOption();

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 189,
			value = "Using a knn predicate in the nested context when tenant or routing filters are required "
					+ "will lead to unpredictable results and may return fewer documents then expected.")
	void knnUsedInNestedContextRequiresFilters();

	@Message(id = ID_OFFSET + 190, value = "A single-valued highlight projection requested, "
			+ "but the corresponding highlighter does not set number of fragments to 1.")
	SearchException highlighterIncompatibleCardinality();

	@Message(id = ID_OFFSET + 193, value = "Current factory cannot be resocped to '%1$s' as it is scoped to '%2$s'.")
	SearchException incompatibleScopeRootType(@FormatWith(ClassFormatter.class) Class<?> requested,
			@FormatWith(ClassFormatter.class) Class<?> actual);
}
