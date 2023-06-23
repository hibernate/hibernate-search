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
 * easily override the self type and predicate factory type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directtly in user code.
 *
 * @param <S> The self type, i.e. the exposed type of this factory.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
public interface ExtendedSearchAggregationFactory<
		S extends ExtendedSearchAggregationFactory<?, PDF>,
		PDF extends SearchPredicateFactory>
		extends SearchAggregationFactory {

	@Override
	S withRoot(String objectFieldPath);

	@Override
	RangeAggregationFieldStep<PDF> range();

	@Override
	TermsAggregationFieldStep<PDF> terms();
}
