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

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.DefaultSearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryContextImplementor;
import org.hibernate.search.engine.search.dsl.sort.impl.DefaultSearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchSortDslContextImpl;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;


public final class DefaultSearchQueryContext<T, Q, C>
		implements SearchQueryContextImplementor<
				DefaultSearchQueryContext<T, Q, C>,
				Q,
				SearchPredicateFactoryContext,
				SearchSortContainerContext
				> {

	private final IndexSearchScope<C> indexSearchScope;
	private final SearchQueryBuilder<T, C> searchQueryBuilder;
	private final Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory;

	public DefaultSearchQueryContext(IndexSearchScope<C> indexSearchScope, SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory) {
		this.indexSearchScope = indexSearchScope;
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
	}

	@Override
	public DefaultSearchQueryContext<T, Q, C> predicate(SearchPredicate predicate) {
		SearchPredicateBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchPredicateBuilderFactory();
		contribute( factory, predicate );
		return this;
	}

	@Override
	public DefaultSearchQueryContext<T, Q, C> predicate(Function<? super SearchPredicateFactoryContext,
			SearchPredicateTerminalContext> dslPredicateContributor) {
		SearchPredicateBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchPredicateBuilderFactory();
		SearchPredicateFactoryContext factoryContext = new DefaultSearchPredicateFactoryContext<>( factory );
		SearchPredicate predicate = dslPredicateContributor.apply( factoryContext ).toPredicate();
		contribute( factory, predicate );
		return this;
	}

	@Override
	public <T2> T2 extension(SearchQueryContextExtension<T2, Q> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, searchQueryBuilder )
		);
	}

	@Override
	public DefaultSearchQueryContext<T, Q, C> routing(String routingKey) {
		searchQueryBuilder.addRoutingKey( routingKey );
		return this;
	}

	@Override
	public DefaultSearchQueryContext<T, Q, C> routing(Collection<String> routingKeys) {
		routingKeys.forEach( searchQueryBuilder::addRoutingKey );
		return this;
	}

	@Override
	public DefaultSearchQueryContext<T, Q, C> sort(SearchSort sort) {
		SearchSortBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchSortBuilderFactory();
		contribute( factory, sort );
		return this;
	}

	@Override
	public DefaultSearchQueryContext<T, Q, C> sort(Consumer<? super SearchSortContainerContext> dslSortContributor) {
		SearchSortBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchSortBuilderFactory();
		contribute( factory, dslSortContributor );
		return this;
	}

	@Override
	public Q toQuery() {
		return searchQueryBuilder.build( searchQueryWrapperFactory );
	}

	private <B> void contribute(SearchPredicateBuilderFactory<? super C, B> factory, SearchPredicate predicate) {
		factory.contribute( searchQueryBuilder.getQueryElementCollector(), factory.toImplementation( predicate ) );
	}

	private <B> void contribute(SearchSortBuilderFactory<? super C, B> factory, SearchSort sort) {
		factory.toImplementation( sort, b -> factory.contribute( searchQueryBuilder.getQueryElementCollector(), b ) );
	}

	private <B> void contribute(SearchSortBuilderFactory<? super C, B> factory,
			Consumer<? super SearchSortContainerContext> dslSortContributor) {
		C collector = searchQueryBuilder.getQueryElementCollector();

		SearchSortDslContextImpl<B> rootDslContext = new SearchSortDslContextImpl<>( factory );
		SearchSortContainerContext containerContext =
				new DefaultSearchSortContainerContext<>( factory, rootDslContext );
		dslSortContributor.accept( containerContext );

		for ( B builder : rootDslContext.getResultingBuilders() ) {
			factory.contribute( collector, builder );
		}
	}

}
