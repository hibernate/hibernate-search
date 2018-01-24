/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when starting to define a nested predicate.
 *
 * @param <N> The type of the next context (returned after the nested query was defined).
 */
public interface NestedPredicateContext<N> {

	NestedPredicateFieldContext<N> onObjectField(String fieldName);

}
