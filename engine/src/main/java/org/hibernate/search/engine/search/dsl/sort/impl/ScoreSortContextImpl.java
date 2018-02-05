/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

class ScoreSortContextImpl<N, C> implements ScoreSortContext<N>, SearchSortContributor<C> {

	private final SearchSortContainerContext<N> containerContext;
	private final Supplier<N> nextContextProvider;
	private final ScoreSortBuilder<C> builder;

	ScoreSortContextImpl(SearchSortContainerContext<N> containerContext,
			SearchSortFactory<C> factory, Supplier<N> nextContextProvider) {
		this.containerContext = containerContext;
		this.nextContextProvider = nextContextProvider;
		this.builder = factory.score();
	}

	@Override
	public ScoreSortContext<N> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public SearchSortContainerContext<N> then() {
		return containerContext;
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}

	@Override
	public void contribute(C collector) {
		builder.contribute( collector );
	}
}
