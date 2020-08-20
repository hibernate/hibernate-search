/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.annotations.Spatial;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SortDistanceNoFieldContext {

	/**
	 * Order elements by distance computed from the coordinates carried by the given field.
	 * <p>The distance is computed between the value of the given field (which must be
	 * a {@link Spatial} field) and reference coordinates, to be provided in the
	 * {@link SortDistanceFieldContext next context}.
	 * @param fieldName The name of the index field carrying the spatial coordinates.
	 */
	SortDistanceFieldContext onField(String fieldName);

}
