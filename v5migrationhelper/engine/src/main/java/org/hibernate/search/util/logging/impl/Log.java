/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.util.logging.impl;

import java.io.IOException;
import java.util.Collection;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log abstraction layer for Hibernate Search on top of JBoss Logging.
 *
 * @author Davide D'Alto
 * @since 4.0
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BaseHibernateSearchLogger {

	@Message(id = 109, value = "%1$s is not an indexed type")
	IllegalArgumentException notAnIndexedType(String entityName);

	@Message(id = 110, value = "'null' is not a valid indexed type")
	IllegalArgumentException nullIsInvalidIndexedType();

	@Message(id = 178, value = "Unable to create a FullTextSession from a null Session")
	IllegalArgumentException getNullSessionPassedToFullTextSessionCreationException();

	@Message(id = 179, value = "Unable to create a FullTextEntityManager from a null EntityManager")
	IllegalArgumentException getNullEntityManagerPassedToFullEntityManagerCreationException();

	@Message(id = 201, value = "The edit distance must be either 1 or 2")
	SearchException incorrectEditDistance();

	@Message(id = 227,
			value = "A BooleanQuery is not valid without at least one clause. Use at least one of should(Query) or must(Query).")
	SearchException booleanQueryWithoutClauses();

	@Message(id = 237, value = "Cannot create numeric range query for field '%s', since from and to values are null")
	SearchException rangeQueryWithNullToAndFromValue(String fieldName);

	@Message(id = 238,
			value = "Cannot create numeric range query for field '%s', since values are not numeric (Date, int, long, short or double)")
	SearchException numericRangeQueryWithNonNumericToAndFromValues(String fieldName);

	@Message(id = 269,
			value = "'%1$s' is not a supported type for a range faceting request parameter. Supported types are: '%2$s'")
	SearchException unsupportedParameterTypeForRangeFaceting(String facetRangeParameterType, String supportedTypes);

	@Message(id = 270, value = "At least one of the facets ranges in facet request '%1$s' contains neither start nor end value")
	SearchException noStartOrEndSpecifiedForRangeQuery(String facetRequestName);

	@Message(id = 271, value = "RANGE_DEFINITION_ORDER is not a valid sort order for a discrete faceting request.")
	SearchException rangeDefinitionOrderRequestedForDiscreteFacetRequest();

	@Message(id = 317, value = "Projection constant '%s' is not supported for this query.")
	SearchException unexpectedProjectionConstant(String constantName);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 336,
			value = "A file could not be deleted: likely lock contention. Not a problem for index replications as it will be attempted again in the future.")
	void fileDeleteFailureIgnored(@Cause IOException e);

	@Message(id = 342, value = "Field '%1$s' refers to both an analyzer and a normalizer.")
	SearchException cannotReferenceAnalyzerAndNormalizer(String relativeFieldPath);

	@Message(id = 352, value = "Multiple conflicting minimumShouldMatch constraints")
	SearchException minimumShouldMatchConflictingConstraints();

	@Message(id = 353, value = "Unknown analyzer: '%1$s'. Make sure you defined this analyzer.")
	SearchException unknownAnalyzer(String analyzerName);

	@Message(id = 400, value = "Cannot unwrap a SearchFactory into a '%1$s'.")
	SearchException cannotUnwrapSearchFactory(@FormatWith(ClassFormatter.class) Class<?> cls);

	@Message(id = 401, value = "Cannot use firstResult > 0 with scrolls.")
	SearchException cannotUseSetFirstResultWithScroll();

	@Message(id = 402, value = "indexNullAs can no longer be set to `DEFAULT_NULL_TOKEN`."
			+ " Specify a value that can be parsed into the property type (%1$s).")
	SearchException defaultNullTokenNotSupported(@FormatWith(ClassFormatter.class) Class<?> propertyType);

	@Message(id = 403, value = "Cannot apply analyzer '%1$s' on a sortable field."
			+ " If you don't need an analyzer, use @Field(analyze = Analyze.NO)."
			+ " If you need to normalize text without tokenizing it, use a normalizer instead: @Field(normalizer = ...)."
			+ " If you need an actual analyzer (with tokenization), define two separate fields:"
			+ " one with an analyzer that is not sortable, and one with a normalizer that is sortable.")
	SearchException cannotUseAnalyzerOnSortableField(String analyzerName);

	@Message(id = 404, value = "indexNullAs is not supported for analyzed fields."
			+ " Trying to define the analyzer: '%1$s' together with indexNullAs: '%2$s'.")
	SearchException cannotUseIndexNullAsAndAnalyzer(String analyzerName, String indexNullAs);

	@Message(id = 406, value = "For simple query string queries, if one field has its analyzer overridden," +
			" all fields must have the same analyzers." +
			" You probably forgot to override the analyzer for some fields," +
			" because multiple analyzers were found: %1$s.")
	SearchException unableToOverrideQueryAnalyzerWithMoreThanOneAnalyzerForSimpleQueryStringQueries(
			Collection<String> analyzers);

	@Message(id = 407, value = "Cannot apply an analyzer on a faceted field. Use a normalizer instead. Analyzer: '%1$s'."
			+ " If an actual analyzer (with tokenization) is necessary, define two separate fields:"
			+ " one with an analyzer and no corresponding @Facet,"
			+ " and one with a normalizer and corresponding @Facet(forField = ...).")
	SearchException cannotUseAnalyzerOnFacetField(String analyzerName);
}
