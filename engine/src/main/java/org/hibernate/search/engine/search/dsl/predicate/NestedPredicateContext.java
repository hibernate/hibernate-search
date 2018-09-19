/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;

/**
 * The context used when starting to define a nested predicate.
 *
 * @param <N> The type of the next context (returned after the nested query was defined).
 */
public interface NestedPredicateContext<N> {

	/**
	 * Set the object field to "nest" on.
	 * <p>
	 * The selected field must be stored as {@link ObjectFieldStorage#NESTED} in the targeted indexes.
	 *
	 * @param absoluteFieldPath The path to the object.
	 * @return A {@link NestedPredicateFieldContext} allowing to define the predicate
	 * that should match on a single object.
	 */
	NestedPredicateFieldContext<N> onObjectField(String absoluteFieldPath);

}
