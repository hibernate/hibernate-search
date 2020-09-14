/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.hibernate.search.query.facet.FacetSortOrder;

/**
 * @author Hardy Ferentschik
 */
public interface FacetParameterContext extends FacetTermination {
	/**
	 * @param sort the sort order for the returned facets.
	 *
	 * @return a {@code FacetParameterContext} to continue building the facet request
	 */
	FacetParameterContext orderedBy(FacetSortOrder sort);

	/**
	 * @param zeroCounts Determines whether values with zero counts are included into the facet result
	 *
	 * @return a {@code FacetParameterContext} to continue building the facet request
	 */
	FacetParameterContext includeZeroCounts(boolean zeroCounts);

	/**
	 * Limits the maximum numbers of facets to the specified number.
	 *
	 * @param maxFacetCount the maximum number of facets to include in the response. A negative value means that
	 * all facets will be included
	 * @return a {@code FacetParameterContext} to continue building the facet request
	 */
	FacetParameterContext maxFacetCount(int maxFacetCount);
}


