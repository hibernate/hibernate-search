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
package org.hibernate.search.query.facet;

import java.util.List;

/**
 * Groups a set of {@link org.hibernate.search.query.facet.Facet} to be applied onto a query.
 * The facet criteria within a {@code FacetSelection} are combined in a disjunction (logical OR).
 *
 * @author Hardy Ferentschik
 */
public interface FacetSelection {
	/**
	 * @param facets An array of facets which have to be applied as disjunction onto the current query
	 */
	void selectFacets(Facet... facets);

	/**
	 * @return returns an unmodifiable list of the currently selected facets
	 */
	List<Facet> getSelectedFacets();

	/**
	 * @param facets An array of facets to e removed from the current facet. Facets which were not part of this
	 * selection will be ignored.
	 */
	void deselectFacets(Facet... facets);

	/**
	 * Clear all facets in this selection
	 */
	void clearSelectedFacets();
}
