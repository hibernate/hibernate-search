/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

/**
 * A DSL context used when building a {@link SearchSort} object,
 * either when calling {@link IndexSearchTarget#sort()} from a search target
 * or when calling {@link SearchQueryContext#sort(Consumer)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchSort} object and cache it).
 */
public final class BuildingRootSearchSortDslContextImpl<C>
		implements SearchSortDslContext<SearchSort, C>, SearchSortContributor<C> {

	private final SearchSortFactory<C> factory;

	private List<SearchSortContributor<? super C>> sortContributors = new ArrayList<>();

	public BuildingRootSearchSortDslContextImpl(SearchSortFactory<C> factory) {
		this.factory = factory;
	}

	@Override
	public void addContributor(SearchSortContributor<? super C> child) {
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
