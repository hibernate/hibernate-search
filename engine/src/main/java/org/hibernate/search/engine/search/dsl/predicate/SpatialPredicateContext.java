/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import org.hibernate.search.engine.search.dsl.ExplicitEndContext;

/**
 * The context used when starting to define a spatial predicate.
 *
 * @param <N> The type of the next context.
 */
public interface SpatialPredicateContext<N> {

	/**
	 * Match documents where targeted fields point to a location within given bounds:
	 * a circle (maximum distance matching), a polygon, a bounding box, ...
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link ExplicitEndContext#end() end the predicate definition}.
	 */
	SpatialWithinPredicateContext<N> within();

}
