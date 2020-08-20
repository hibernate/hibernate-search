/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.facet;

/**
 * Specifies the order in which the facets are returned.
 *
 * @author Hardy Ferentschik
 */
public enum FacetSortOrder {
	/**
	 * Facets are returned by count with the lowest count first
	 */
	COUNT_ASC,

	/**
	 * Facets are returned by count with the lowest count first
	 */
	COUNT_DESC,

	/**
	 * Facets are returned in the alphabetical order
	 */
	FIELD_VALUE,

	/**
	 * The order in which ranges were defined. Only valid for range faceting
	 */
	RANGE_DEFINITION_ORDER
}
