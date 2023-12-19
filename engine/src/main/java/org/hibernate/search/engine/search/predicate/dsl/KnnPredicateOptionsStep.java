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
public interface KnnPredicateOptionsStep<S extends KnnPredicateOptionsStep<?>>
		extends PredicateScoreStep<S>, PredicateFinalStep {

	S filter(SearchPredicate searchPredicate);

	default S filter(PredicateFinalStep searchPredicate) {
		return filter( searchPredicate.toPredicate() );
	}

	S filter(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);
}
