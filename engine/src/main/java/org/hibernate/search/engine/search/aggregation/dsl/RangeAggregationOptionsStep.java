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
 * The final step in a "range" aggregation definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 * @param <F> The type of the targeted field.
 * @param <A> The type of result for this aggregation.
 */
public interface RangeAggregationOptionsStep<
		S extends RangeAggregationOptionsStep<?, PDF, F, A>,
		PDF extends SearchPredicateFactory,
		F,
		A>
		extends AggregationFinalStep<A>, AggregationFilterStep<S, PDF> {

}
