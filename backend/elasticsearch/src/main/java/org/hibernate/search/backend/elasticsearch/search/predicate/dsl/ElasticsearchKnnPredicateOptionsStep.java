/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.dsl;

import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;

/**
 * The final step in a "knn" predicate definition, where optional parameters can be set.
 */
public interface ElasticsearchKnnPredicateOptionsStep<T extends ElasticsearchKnnPredicateOptionsStep<?>>
		extends KnnPredicateOptionsStep<T> {

	/**
	 * Set the number of approximate nearest neighbor candidates on each shard.
	 * <p>
	 * An Elasticsearch specific option of an Elasticsearch distribution. Setting this on any other distribution
	 * will lead to an exception being thrown.
	 * See also <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html#tune-approximate-knn-for-speed-accuracy">Tune approximate kNN for speed or accuracy</a>.
	 * @param numberOfCandidates The number of candidates per shard to consider.
	 * @return {@code this}, for method chaining.
	 */
	ElasticsearchKnnPredicateOptionsStep<?> numberOfCandidates(int numberOfCandidates);

}
