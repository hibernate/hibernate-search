/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "knn" predicate definition where the vector to match is defined.
 */
public interface KnnPredicateVectorStep {
	/**
	 * @param vector The vector from which to compute the distance to vectors in the indexed field.
	 * @return The next step in the knn predicate DSL.
	 */
	KnnPredicateOptionsStep matching(byte... vector);

	/**
	 * @param vector The vector from which to compute the distance to vectors in the indexed field.
	 * @return The next step in the knn predicate DSL.
	 */
	KnnPredicateOptionsStep matching(float... vector);

}
