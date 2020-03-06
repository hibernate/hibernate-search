/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;
import org.hibernate.search.engine.search.common.MultiValue;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The final step in a "range" aggregation definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of the targeted field.
 * @param <A> The type of result for this aggregation.
 */
public interface RangeAggregationOptionsStep<S extends RangeAggregationOptionsStep<?, F, A>, F, A>
	extends AggregationFinalStep<A> {

	/**
	 * Start describing the behavior of this sort when a document do have
	 * multi values for the targeted field.
	 *
	 * @param mode The renge.
	 * @return The next step.
	 */
	S mode(MultiValue mode);

	/**
	 * Add a <a href="#filter">"filter" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must match.
	 * @return {@code this}, for method chaining.
	 */
	S filter(SearchPredicate searchPredicate);

	/**
	 * Add a <a href="#filter">"filter" clause</a> to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S filter(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Add a <a href="#filter">"filter" clause</a> based on an almost-built {@link SearchPredicate}.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return {@code this}, for method chaining.
	 */
	default S filter(PredicateFinalStep dslFinalStep) {
		return filter( dslFinalStep.toPredicate() );
	}
}
