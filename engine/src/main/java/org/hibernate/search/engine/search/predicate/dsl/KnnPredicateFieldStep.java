/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.KnnPredicateFieldReference;

/**
 * The initial step in a "knn" predicate definition, where the target field can be set.
 * @param <SR> Scope root type.
 */
public interface KnnPredicateFieldStep<SR> {

	/**
	 * Target the given field in the match predicate.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field to apply the predicate on.
	 * @return The next step in the knn predicate DSL.
	 */
	KnnPredicateVectorStep<SR> field(String fieldPath);

	<T> KnnPredicateVectorGenericStep<SR, T> field(KnnPredicateFieldReference<? super SR, T> field);
}
