/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "knn" predicate definition where the vector to match is defined.
 */
public interface KnnPredicateVectorGenericStep<SR, T> {
	/**
	 * @param vector The vector from which to compute the distance to vectors in the indexed field.
	 * @return The next step in the knn predicate DSL.
	 */
	KnnPredicateOptionsStep<SR> matching(T vector);

}
