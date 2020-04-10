/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * A base interface for subtypes of {@link SearchAggregationFactory} allowing to
 * easily override the predicate factory type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directtly in user code.
 *
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
public interface ExtendedSearchAggregatonFactory<PDF extends SearchPredicateFactory>
	extends SearchAggregationFactory {

	/**
	 * Perform aggregation in range buckets.
	 * <p>
	 * Given a field and one or more ranges of values,
	 * this aggregation creates one bucket per range,
	 * and puts in each bucket every document for which
	 * the given field has a value that falls into the corresponding range.
	 * <p>
	 * For each bucket, the document count is computed,
	 * or more complex metrics or sub-aggregations for backends that support it.
	 *
	 * @return The next step.
	 */
	@Override
	RangeAggregationFieldStep<PDF> range();

	/**
	 * Perform aggregation in term buckets.
	 * <p>
	 * Given a field,
	 * this aggregation creates one bucket per term of that field in the index,
	 * and puts in each bucket every document for which
	 * the given field matches the corresponding term.
	 * <p>
	 * For each bucket, the document count is computed,
	 * or more complex metrics or sub-aggregations for backends that support it.
	 *
	 * @return The next step.
	 */
	@Override
	TermsAggregationFieldStep<PDF> terms();
}
