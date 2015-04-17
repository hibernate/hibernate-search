/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * @param facets An array of facets which have to be applied as disjunction onto the current query. Facets are combined
	 * via {@link FacetCombine#OR}.
	 */
	void selectFacets(Facet... facets);

	/**
	 * @param combineBy enum value defining how the different facet should be combined when building a boolean query which
	 * gets applied onto the current query
	 * @param facets An array of facets which have to be applied as disjunction onto the current query
	 */
	void selectFacets(FacetCombine combineBy, Facet... facets);

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
