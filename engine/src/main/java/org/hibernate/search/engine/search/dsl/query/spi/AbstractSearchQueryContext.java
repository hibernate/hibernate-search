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
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortTerminalContext;
import org.hibernate.search.engine.search.dsl.sort.impl.DefaultSearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchSortDslContextImpl;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

public abstract class AbstractSearchQueryContext<
				S extends SearchQueryContext<S, H, SC>,
				H,
				PDF extends SearchPredicateFactory,
				SC extends SearchSortFactoryContext,
				C
		>
		implements SearchQueryResultContext<S, H, PDF>, SearchQueryContext<S, H, SC> {

	private final IndexScope<C> indexScope;
	private final SearchQueryBuilder<H, C> searchQueryBuilder;

	public AbstractSearchQueryContext(IndexScope<C> indexScope,
			SearchQueryBuilder<H, C> searchQueryBuilder) {
		this.indexScope = indexScope;
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public S predicate(SearchPredicate predicate) {
		SearchPredicateBuilderFactory<? super C, ?> factory = indexScope.getSearchPredicateBuilderFactory();
		contribute( factory, predicate );
		return thisAsS();
	}

	@Override
	public S predicate(Function<? super PDF, ? extends PredicateFinalStep> predicateContributor) {
		SearchPredicateBuilderFactory<? super C, ?> builderFactory = indexScope.getSearchPredicateBuilderFactory();
		SearchPredicateFactory factory = new DefaultSearchPredicateFactory<>( builderFactory );
		SearchPredicate predicate = predicateContributor.apply( extendPredicateFactory( factory ) ).toPredicate();
		contribute( builderFactory, predicate );
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
		SearchSortBuilderFactory<? super C, ?> factory = indexScope.getSearchSortBuilderFactory();
		contribute( factory, sort );
		return thisAsS();
	}

	@Override
	public S sort(Function<? super SC, ? extends SearchSortTerminalContext> sortContributor) {
		SearchSortBuilderFactory<? super C, ?> factory = indexScope.getSearchSortBuilderFactory();
		SearchSortFactoryContext factoryContext = new DefaultSearchSortFactoryContext<>(
				SearchSortDslContextImpl.root( factory )
		);
		SearchSort sort = sortContributor.apply( extendSortContext( factoryContext ) ).toSort();
		contribute( factory, sort );
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
		factory.contribute( searchQueryBuilder.getQueryElementCollector(), factory.toImplementation( sort ) );
	}

	protected abstract S thisAsS();

	protected abstract PDF extendPredicateFactory(SearchPredicateFactory predicateFactory);

	protected abstract SC extendSortContext(SearchSortFactoryContext sortFactoryContext);
}
