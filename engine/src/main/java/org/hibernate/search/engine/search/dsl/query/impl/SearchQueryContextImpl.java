/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.Collection;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;


/**
 * @author Yoann Rodiere
 */
public final class SearchQueryContextImpl<T, Q, C> implements SearchQueryContext<Q> {

	private final SearchQueryBuilder<T, C> searchQueryBuilder;

	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;

	public SearchQueryContextImpl(SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
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
	public Q build() {
		return searchQueryBuilder.build( searchQueryWrapperFactory );
	}

}
