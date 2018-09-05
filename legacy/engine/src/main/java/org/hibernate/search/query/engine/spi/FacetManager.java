/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import java.util.List;

import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSelection;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * Interface defining methods around faceting.
 *
 * @author Hardy Ferentschik
 */
public interface FacetManager {
	/**
	 * Enable a facet request.
	 *
	 * @param facetingRequest the faceting request
	 * @return {@code this} to allow method chaining
	 */
	FacetManager enableFaceting(FacetingRequest facetingRequest);

	/**
	 * Disable a facet with the given name.
	 *
	 * @param facetingName the name of the facet to disable.
	 */
	void disableFaceting(String facetingName);

	/**
	 * Returns the {@code Facet}s for a given facet name
	 *
	 * @param facetingName the facet name for which to return the facet list
	 * @return the facet result list which corresponds to the facet request with the given name. The empty list
	 *         is returned for an unknown facet name.
	 * @see #enableFaceting(org.hibernate.search.query.facet.FacetingRequest)
	 */
	List<Facet> getFacets(String facetingName);

	/**
	 * Returns a instance of {@code FacetSelection} instance in order to apply a disjunction of facet criteria on
	 * the current query.
	 *
	 * @param groupName the name the group. If the name is unknown an empty selection group is returned. {@code null}
	 * is not allowed.
	 * @return the {@code FacetSelection} for this group name if it exists., otherwise a new selection is created.
	 */
	FacetSelection getFacetGroup(String groupName);
}
