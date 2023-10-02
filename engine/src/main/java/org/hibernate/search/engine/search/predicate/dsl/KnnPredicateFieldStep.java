/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial step in a "knn" predicate definition, where the target field can be set.
 */
public interface KnnPredicateFieldStep {

	/**
	 * Target the given field in the match predicate.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field to apply the predicate on.
	 * @return The next step in the knn predicate DSL.
	 */
	KnnPredicateVectorStep field(String fieldPath);
}
