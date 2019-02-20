/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The terminal context of the predicate DSL.
 */
public interface SpatialPredicateContext extends SearchPredicateNoFieldContext<SpatialPredicateContext> {

	/**
	 * Match documents where targeted fields point to a location within given bounds:
	 * a circle (maximum distance matching), a polygon, a bounding box, ...
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 */
	SpatialWithinPredicateContext within();

}
