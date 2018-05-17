/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when for closing a within circle spatial predicate.
 *
 * @param <N> The type of the next context.
 */
public interface SpatialWithinCirclePredicateContext<N> {

	N end();
}
