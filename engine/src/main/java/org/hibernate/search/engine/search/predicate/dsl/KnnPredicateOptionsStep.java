/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * The final step in a "knn" predicate definition, where optional parameters can be set.
 */
public interface KnnPredicateOptionsStep<E>
		extends PredicateScoreStep<KnnPredicateOptionsStep<E>>, PredicateFinalStep {

	KnnPredicateOptionsStep<E> filter(SearchPredicate searchPredicate);

	default KnnPredicateOptionsStep<E> filter(PredicateFinalStep searchPredicate) {
		return filter( searchPredicate.toPredicate() );
	}

	KnnPredicateOptionsStep<E> filter(
			Function<? super SearchPredicateFactory<E>, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * @param similarity A similarity limit: documents with vectors distance to which, according to the configured similarity function,
	 * is further than this limit will be filtered out from the results.
	 * @return {@code this}, for method chaining.
	 */
	KnnPredicateOptionsStep<E> requiredMinimumSimilarity(float similarity);

}
