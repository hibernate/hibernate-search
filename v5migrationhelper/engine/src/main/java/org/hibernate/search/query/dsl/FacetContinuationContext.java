/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * @author Hardy Ferentschik
 * @deprecated See the deprecation note on {@link FacetContext}.
 */
@Deprecated
public interface FacetContinuationContext {
	/**
	 * Start building a range request
	 *
	 * @param <T> the type of the values in the range
	 * @return a {@code FacetRangeContext} to continue building the facet request
	 */
	<T> FacetRangeAboveBelowContext<T> range();

	/**
	 * Start building a discrete facet
	 *
	 * @return  a {@code FacetRangeContext} to continue building the facet request
	 */
	DiscreteFacetContext discrete();
}

