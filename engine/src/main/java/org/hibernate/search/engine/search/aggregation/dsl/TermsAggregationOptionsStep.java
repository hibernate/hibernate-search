/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The final step in a "terms" aggregation definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of the targeted field.
 * @param <A> The type of result for this aggregation.
 */
public interface TermsAggregationOptionsStep<S extends TermsAggregationOptionsStep<?, F, A>, F, A>
		extends AggregationFinalStep<A> {

	/**
	 * Order buckets by descending document count in the aggregation result.
	 * <p>
	 * This is the default behavior.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S orderByCountDescending();

	/**
	 * Order buckets by ascending document count in the aggregation result.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S orderByCountAscending();

	/**
	 * Order buckets by ascending term value in the aggregation result.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S orderByTermAscending();

	/**
	 * Order buckets by descending term value in the aggregation result.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S orderByTermDescending();

	/**
	 * Eliminates buckets with less than {@code minDocumentCount} matching documents
	 * from the aggregation result.
	 * <p>
	 * If set to {@code 0}, terms that are present in the index,
	 * but are not referenced in any document matched by the search query
	 * will yield a bucket with a document count of zero.
	 * <p>
	 * Defaults to {@code 1}.
	 *
	 * @param minDocumentCount The minimum document count for each aggregation value.
	 * @return {@code this}, for method chaining.
	 */
	S minDocumentCount(int minDocumentCount);

	/**
	 * Requires to only create buckets for the top {@code maxTermCount} most frequent terms.
	 * <p>
	 * Defaults to {@code 100}.
	 *
	 * @param maxTermCount The maximum number of reported terms.
	 * @return {@code this}, for method chaining.
	 */
	S maxTermCount(int maxTermCount);

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
