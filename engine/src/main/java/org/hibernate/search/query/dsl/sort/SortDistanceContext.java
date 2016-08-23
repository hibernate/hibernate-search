/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.query.dsl.Unit;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortDistanceContext extends SortOrder<SortDistanceContext>, SortAdditionalSortFieldContext, SortTermination {

	/**
	 * Hint for the backend to return data in the appropriate unit.
	 */
	SortDistanceContext in(Unit unit);

	/**
	 * Hint for the backend to use a specific distance algorithm
	 */
	SortDistanceContext withComputeMethod(DistanceMethod distanceMethod);

	// parametrization

	SortMissingValueContext<SortDistanceContext> onMissingValue();
}
