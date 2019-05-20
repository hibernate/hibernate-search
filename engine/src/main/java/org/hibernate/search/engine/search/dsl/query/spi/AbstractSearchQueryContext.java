/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.DefaultSearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.DefaultSearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchSortDslContextImpl;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

public abstract class AbstractSearchQueryContext<
		S extends SearchQueryContext<S, H, SC>,
		H,
		PDC extends SearchPredicateFactoryContext,
		SC extends SearchSortContainerContext,
		C
		>
		implements SearchQueryResultContext<S, H, PDC>, SearchQueryContext<S, H, SC> {

	private final IndexSearchScope<C> indexSearchScope;
	private final SearchQueryBuilder<H, C> searchQueryBuilder;

	public AbstractSearchQueryContext(IndexSearchScope<C> indexSearchScope,
			SearchQueryBuilder<H, C> searchQueryBuilder) {
		this.indexSearchScope = indexSearchScope;
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public S predicate(SearchPredicate predicate) {
		SearchPredicateBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchPredicateBuilderFactory();
		contribute( factory, predicate );
		return thisAsS();
	}

	@Override
	public S predicate(Function<? super PDC, SearchPredicateTerminalContext> predicateContributor) {
		SearchPredicateBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchPredicateBuilderFactory();
		SearchPredicateFactoryContext factoryContext = new DefaultSearchPredicateFactoryContext<>( factory );
		SearchPredicate predicate = predicateContributor.apply( extendPredicateContext( factoryContext ) ).toPredicate();
		contribute( factory, predicate );
		return thisAsS();
	}

	@Override
	public S routing(String routingKey) {
		searchQueryBuilder.addRoutingKey( routingKey );
		return thisAsS();
	}

	@Override
	public S routing(Collection<String> routingKeys) {
		routingKeys.forEach( searchQueryBuilder::addRoutingKey );
		return thisAsS();
	}

	@Override
	public S sort(SearchSort sort) {
		SearchSortBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchSortBuilderFactory();
		contribute( factory, sort );
		return thisAsS();
	}

	@Override
	public S sort(Consumer<? super SC> sortContributor) {
		SearchSortBuilderFactory<? super C, ?> factory = indexSearchScope.getSearchSortBuilderFactory();
		contribute( factory, sortContributor );
		return thisAsS();
	}

	@Override
	public SearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	public SearchResult<H> fetch() {
		return toQuery().fetch();
	}

	@Override
	public SearchResult<H> fetch(Integer limit) {
		return toQuery().fetch( limit );
	}

	@Override
	public SearchResult<H> fetch(Integer limit, Integer offset) {
		return toQuery().fetch( limit, offset );
	}

	@Override
	public List<H> fetchHits() {
		return toQuery().fetchHits();
	}

	@Override
	public List<H> fetchHits(Integer limit) {
		return toQuery().fetchHits( limit );
	}

	@Override
	public List<H> fetchHits(Integer limit, Integer offset) {
		return toQuery().fetchHits( limit, offset );
	}

	@Override
	public Optional<H> fetchSingleHit() {
		return toQuery().fetchSingleHit();
	}

	@Override
	public long fetchTotalHitCount() {
		return toQuery().fetchTotalHitCount();
	}

	private <B> void contribute(SearchPredicateBuilderFactory<? super C, B> factory, SearchPredicate predicate) {
		factory.contribute( searchQueryBuilder.getQueryElementCollector(), factory.toImplementation( predicate ) );
	}

	private <B> void contribute(SearchSortBuilderFactory<? super C, B> factory, SearchSort sort) {
		factory.toImplementation( sort, b -> factory.contribute( searchQueryBuilder.getQueryElementCollector(), b ) );
	}

	private <B> void contribute(SearchSortBuilderFactory<? super C, B> factory,
			Consumer<? super SC> dslSortContributor) {
		C collector = searchQueryBuilder.getQueryElementCollector();

		SearchSortDslContextImpl<B> rootDslContext = new SearchSortDslContextImpl<>( factory );
		SearchSortContainerContext containerContext =
				new DefaultSearchSortContainerContext<>( factory, rootDslContext );
		dslSortContributor.accept( extendSortContext( containerContext ) );

		for ( B builder : rootDslContext.getResultingBuilders() ) {
			factory.contribute( collector, builder );
		}
	}

	protected abstract S thisAsS();

	protected abstract PDC extendPredicateContext(SearchPredicateFactoryContext predicateFactoryContext);

	protected abstract SC extendSortContext(SearchSortContainerContext sortContainerContext);
}
