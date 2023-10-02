/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * The final step in a "knn" predicate definition, where optional parameters can be set.
 */
public interface KnnPredicateOptionsStep
		extends PredicateScoreStep<KnnPredicateOptionsStep>, PredicateFinalStep {

	KnnPredicateOptionsStep filter(SearchPredicate searchPredicate);

	default KnnPredicateOptionsStep filter(PredicateFinalStep searchPredicate) {
		return filter( searchPredicate.toPredicate() );
	}

	KnnPredicateOptionsStep filter(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * @param similarity A similarity limit: documents with vectors distance to which, according to the configured similarity function,
	 * is further than this limit will be filtered out from the results.
	 * @return {@code this}, for method chaining.
	 */
	KnnPredicateOptionsStep requiredMinimumSimilarity(float similarity);

}
