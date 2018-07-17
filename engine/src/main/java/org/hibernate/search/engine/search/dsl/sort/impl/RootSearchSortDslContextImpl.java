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
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

/**
 * A DSL context used when building a {@link SearchSort} object,
 * either when calling {@link IndexSearchTarget#sort()} from a search target
 * or when calling {@link SearchQueryContext#sort(Consumer)} to build the sort using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchSort} object and cache it).
 */
public final class RootSearchSortDslContextImpl<B>
		implements SearchSortDslContext<SearchSort, B> {

	private final SearchSortFactory<?, B> factory;

	private final SearchSortContributorAggregator<B> aggregator = new SearchSortContributorAggregator<>();

	private SearchSort sortResult;

	public RootSearchSortDslContextImpl(SearchSortFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public void addChild(SearchSortContributor<? extends B> contributor) {
		aggregator.add( contributor );
	}

	@Override
	public SearchSort getNextContext() {
		if ( sortResult == null ) {
			List<B> builderResult = new ArrayList<>();
			aggregator.contribute( builderResult::add );
			sortResult = factory.toSearchSort( builderResult );
		}
		return sortResult;
	}

	public List<B> getResultingBuilders() {
		List<B> builderResult = new ArrayList<>();
		if ( sortResult != null ) {
			/*
			 * If the SearchSort object was already created,
			 * we can't use the builders collected by the aggregator anymore: they might be single-use.
			 * We just ask the factory to convert the SearchSort object back to builders.
			 * If the builders can be used multiple times, the factory can optimize this.
			 */
			factory.toImplementation( sortResult, builderResult::add );
		}
		else {
			/*
			 * Optimization: we know the user will not be able to request a SearchSort object anymore,
			 * so we don't need to build a SearchSort object in this case,
			 * we can just use the builders collected by the aggregator directly.
			 */
			aggregator.contribute( builderResult::add );
		}
		return builderResult;
	}
}
