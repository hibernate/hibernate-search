/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.BuildingRootSearchSortDslContextImpl;
import org.hibernate.search.engine.search.dsl.sort.impl.QuerySearchSortDslContextImpl;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchSortContainerContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;


/**
 * @author Yoann Rodiere
 */
public final class SearchQueryContextImpl<T, Q, C> implements SearchQueryContext<Q> {

	private final SearchTargetContext<C> targetContext;
	private final SearchQueryBuilder<T, C> searchQueryBuilder;
	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;

	public SearchQueryContextImpl(SearchTargetContext<C> targetContext, SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		this.targetContext = targetContext;
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
	}

	@Override
	public SearchQueryContext<Q> routing(String routingKey) {
		searchQueryBuilder.addRoutingKey( routingKey );
		return this;
	}

	@Override
	public SearchQueryContext<Q> routing(Collection<String> routingKeys) {
		routingKeys.forEach( searchQueryBuilder::addRoutingKey );
		return this;
	}

	@Override
	public SearchQueryContext<Q> sort(SearchSort sort) {
		SearchSortFactory<? super C> factory = targetContext.getSearchSortFactory();
		factory.toContributor( sort ).contribute( searchQueryBuilder.getQueryElementCollector() );
		return this;
	}

	@Override
	public SearchQueryContext<Q> sort(Consumer<? super SearchSortContainerContext<SearchSort>> dslSortContributor) {
		toContributor( targetContext.getSearchSortFactory(), dslSortContributor )
				.contribute( searchQueryBuilder.getQueryElementCollector() );
		return this;
	}

	@Override
	public SearchSortContainerContext<SearchQueryContext<Q>> sort() {
		SearchSortDslContext<SearchQueryContext<Q>, C> dslContext =
				new QuerySearchSortDslContextImpl<>( searchQueryBuilder.getQueryElementCollector(), this );
		return new SearchSortContainerContextImpl<>( targetContext.getSearchSortFactory(), dslContext );
	}

	private <SC> SearchSortContributor<SC> toContributor(SearchSortFactory<SC> factory,
			Consumer<? super SearchSortContainerContext<SearchSort>> dslSortContributor) {
		BuildingRootSearchSortDslContextImpl<SC> dslContext =
				new BuildingRootSearchSortDslContextImpl<>( factory );
		SearchSortContainerContext<SearchSort> containerContext =
				new SearchSortContainerContextImpl<>( factory, dslContext );
		dslSortContributor.accept( containerContext );
		return dslContext;
	}

	@Override
	public Q build() {
		return searchQueryBuilder.build( searchQueryWrapperFactory );
	}

}
