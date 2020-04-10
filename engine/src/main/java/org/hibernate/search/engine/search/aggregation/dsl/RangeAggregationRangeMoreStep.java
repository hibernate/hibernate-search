/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

import org.hibernate.search.util.common.data.Range;

/**
 * The step in a "range" aggregation definition where optional parameters can be set,
 * (see the superinterface {@link RangeAggregationOptionsStep}),
 * or more ranges can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 * @param <F> The type of the targeted field.
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
*/
public interface RangeAggregationRangeMoreStep<
				S extends RangeAggregationRangeMoreStep<?, ?, F, PDF>,
				N extends RangeAggregationOptionsStep<?, F, Map<Range<F>, Long>, PDF>,
				F,
				PDF extends SearchPredicateFactory
		>
		extends RangeAggregationOptionsStep<N, F, Map<Range<F>, Long>, PDF>,
				RangeAggregationRangeStep<S, F, PDF> {

}
