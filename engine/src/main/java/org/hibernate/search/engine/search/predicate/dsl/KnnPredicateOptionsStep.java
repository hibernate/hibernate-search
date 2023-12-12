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
public interface KnnPredicateOptionsStep extends PredicateScoreStep<KnnPredicateOptionsStep>, PredicateFinalStep {

	KnnPredicateOptionsStep filter(SearchPredicate searchPredicate);

	default KnnPredicateOptionsStep filter(PredicateFinalStep searchPredicate) {
		return filter( searchPredicate.toPredicate() );
	}

	KnnPredicateOptionsStep filter(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);


	/**
	 * Set the number of approximate nearest neighbor candidates on each shard.
	 * <p>
	 * An Elasticsearch specific option of an Elasticsearch distribution. Setting this on any other backends/distributions
	 * will lead to an exception being thrown.
	 * See also <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html#tune-approximate-knn-for-speed-accuracy">Tune approximate kNN for speed or accuracy</a>.
	 * @param numberOfCandidates The number of candidates per shard to consider.
	 * @return {@code this}, for method chaining.
	 */
	KnnPredicateOptionsStep numberOfCandidates(int numberOfCandidates);

}
