/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.logging.impl;

import java.io.IOException;

import org.hibernate.search.util.common.SearchException;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
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

	@Message(id = 227, value = "A BooleanQuery is not valid without at least one clause. Use at least one of should(Query) or must(Query)." )
	SearchException booleanQueryWithoutClauses();

	@Message(id = 237, value = "Cannot create numeric range query for field '%s', since from and to values are null" )
	SearchException rangeQueryWithNullToAndFromValue(String fieldName);

	@Message(id = 238, value = "Cannot create numeric range query for field '%s', since values are not numeric (Date, int, long, short or double)")
	SearchException numericRangeQueryWithNonNumericToAndFromValues(String fieldName);

	@Message(id = 269, value = "'%1$s' is not a supported type for a range faceting request parameter. Supported types are: '%2$s'")
	SearchException unsupportedParameterTypeForRangeFaceting(String facetRangeParameterType, String supportedTypes);

	@Message(id = 270, value = "At least of of the facets ranges in facet request '%1$s' contains neither start nor end value")
	SearchException noStartOrEndSpecifiedForRangeQuery(String facetRequestName);

	@Message(id = 271, value = "RANGE_DEFINITION_ORDER is not a valid sort order for a discrete faceting request.")
	SearchException rangeDefinitionOrderRequestedForDiscreteFacetRequest();

	@Message(id = 317, value = "Projection constant '%s' is not supported for this query.")
	SearchException unexpectedProjectionConstant(String constantName);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 336, value = "A file could not be deleted: likely lock contention. Not a problem for index replications as it will be attempted again in the future.")
	void fileDeleteFailureIgnored(@Cause IOException e);

	@Message(id = 342, value = "Field '%1$s' refers to both an analyzer and a normalizer." )
	SearchException cannotReferenceAnalyzerAndNormalizer(String relativeFieldPath);

	@Message(id = 351, value = "Computed minimum for minimumShouldMatch constraint is out of bounds:"
			+ " expected a number between 1 and '%1$s', got '%2$s'.")
	SearchException minimumShouldMatchMinimumOutOfBounds(int minimum, int totalShouldClauseNumber);

	@Message(id = 352, value = "Multiple conflicting minimumShouldMatch constraints")
	SearchException minimumShouldMatchConflictingConstraints();

	@Message(id = 353, value = "Unknown analyzer: '%1$s'. Make sure you defined this analyzer.")
	SearchException unknownAnalyzerForOverride(String analyzerName);
}
