/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

public final class QuerySearchSortBuildingRootContextImpl<C>
		implements SearchSortDslContext<SearchSort, C>, SearchSortContributor<C> {

	private final SearchSortFactory<C> factory;

	private List<SearchSortContributor<C>> sortContributors = new ArrayList<>();

	public QuerySearchSortBuildingRootContextImpl(SearchSortFactory<C> factory) {
		this.factory = factory;
	}

	@Override
	public void addContributor(SearchSortContributor<C> child) {
		sortContributors.add( child );
	}

	@Override
	public SearchSort getNextContext() {
		return factory.toSearchSort( this );
	}

	@Override
	public void contribute(C collector) {
		sortContributors.forEach( c -> c.contribute( collector ) );
	}
}
