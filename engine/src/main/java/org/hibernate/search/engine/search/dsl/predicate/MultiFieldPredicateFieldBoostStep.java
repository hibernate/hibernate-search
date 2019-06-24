/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The step in a predicate definition where the boost of the last added field(s) can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 */
public interface MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Boost the weight of the last added set of fields in score computation.
	 *
	 * @param boost The boost factor. Higher than 1 increases the weight in score computation,
	 * between 0 and 1 lowers the weight. Lower than 0 is for experts only.
	 * @return {@code this}, for method chaining.
	 */
	S boostedTo(float boost);

}
