/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.facet;

/**
 * Faceting request interface.
 *
 * @author Hardy Ferentschik
 */
public interface FacetingRequest {
	/**
	 * @return the name of this faceting request. The faceting name can be an arbitrary string.
	 */
	String getFacetingName();

	/**
	 * @return the {@code Document} field name on which this faceting request is defined on
	 */
	String getFieldName();

	/**
	 * @return the sort order of the returned {@code Facet}s for this request
	 */
	FacetSortOrder getSort();

	/**
	 * @return the maximum number of facets returned for this request
	 */
	int getMaxNumberOfFacets();

	/**
	 * @return {@code true} if facets with a count of 0 should be included in the returned facet list
	 */
	boolean hasZeroCountsIncluded();
}
