/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public interface FacetTermination {
	/**
	 * @return the {@link FacetingRequest} produced by the building process.
	 */
	FacetingRequest createFacetingRequest();
}
