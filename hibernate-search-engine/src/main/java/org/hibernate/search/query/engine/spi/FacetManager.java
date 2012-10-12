/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
