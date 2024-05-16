/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.traits.predicate.KnnPredicateFieldReference;

/**
 * The initial step in a "knn" predicate definition, where the target field can be set.
 */
public interface KnnPredicateFieldStep<E> {

	/**
	 * Target the given field in the match predicate.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field to apply the predicate on.
	 * @return The next step in the knn predicate DSL.
	 */
	KnnPredicateVectorStep<E> field(String fieldPath);

	<T> KnnPredicateVectorGenericStep<T> field(KnnPredicateFieldReference<E, T> field);
}
