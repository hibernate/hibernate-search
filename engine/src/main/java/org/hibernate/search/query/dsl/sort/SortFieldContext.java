/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
import java.util.Comparator;

import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortFieldContext extends SortAdditionalSortFieldContext, SortOrder<SortFieldContext>, SortTermination {

	// Geospatial sorting

	/**
	 * Order elements by their distance from {@code coordinates}.
	 */
	SortDistanceContext fromCoordinates(Coordinates coordinates);

	/**
	 * Latitude in degree
	 */
	SortLatLongContext fromLatitude(double latitude);

	// native comparator

	/**
	 * Compare field with specific comparator implementation.
	 */
	SortComparatorContext withComparator(Comparator<?> comparator);

	// parametrization

	/**
	 * Describe how to treat missing values when doing the sorting.
	 */
	SortMissingValueContext<SortFieldContext> onMissingValue();

}
