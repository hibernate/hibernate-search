/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;


/**
 * The initial step of all spatial predicate definitions.
 */
public interface SpatialPredicateInitialStep {

	/**
	 * Match documents where targeted fields point to a location within given bounds:
	 * a circle (maximum distance matching), a polygon, a bounding box, ...
	 *
	 * @return The initial step of a DSL allowing the definition of a "within" predicate.
	 */
	SpatialWithinPredicateFieldStep within();

}
