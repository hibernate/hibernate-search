/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.impl.DefaultSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.impl.SearchAggregationDslContextImpl;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryPredicateStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.impl.DefaultSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.impl.SearchSortDslContextImpl;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

public abstract class AbstractSearchQueryOptionsStep<
				S extends SearchQueryOptionsStep<S, H, SF, AF>,
				H,
				PDF extends SearchPredicateFactory,
				SF extends SearchSortFactory,
				AF extends SearchAggregationFactory,
				C
		>
		implements SearchQueryPredicateStep<S, H, PDF>, SearchQueryOptionsStep<S, H, SF, AF> {

	private final IndexScope<C> indexScope;
	private final SearchQueryBuilder<H, C> searchQueryBuilder;

	public AbstractSearchQueryOptionsStep(IndexScope<C> indexScope,
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
	public S sort(Function<? super SF, ? extends SortFinalStep> sortContributor) {
		SearchSortBuilderFactory<? super C, ?> builderFactory = indexScope.getSearchSortBuilderFactory();
		SearchSortFactory factory = new DefaultSearchSortFactory<>(
				SearchSortDslContextImpl.root( builderFactory )
		);
		SearchSort sort = sortContributor.apply( extendSortFactory( factory ) ).toSort();
		contribute( builderFactory, sort );
		return thisAsS();
	}

	@Override
	public <A> S aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation) {
		SearchAggregationBuilderFactory<? super C> builderFactory = indexScope.getSearchAggregationFactory();
		contribute( builderFactory, key, aggregation );
		return thisAsS();
	}

	@Override
	public <A> S aggregation(AggregationKey<A> key, Function<? super AF, ? extends AggregationFinalStep<A>> aggregationContributor) {
		SearchAggregationBuilderFactory<? super C> builderFactory = indexScope.getSearchAggregationFactory();
		AF factory = extendAggregationFactory( new DefaultSearchAggregationFactory(
				SearchAggregationDslContextImpl.root( builderFactory )
		) );
		SearchAggregation<A> aggregation = aggregationContributor.apply( factory ).toAggregation();
		contribute( builderFactory, key, aggregation );
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
		factory.contribute( searchQueryBuilder.toQueryElementCollector(), factory.toImplementation( predicate ) );
	}

	private <B> void contribute(SearchSortBuilderFactory<? super C, B> factory, SearchSort sort) {
		factory.contribute( searchQueryBuilder.toQueryElementCollector(), factory.toImplementation( sort ) );
	}

	private <A> void contribute(SearchAggregationBuilderFactory<? super C> factory,
			AggregationKey<A> aggregationKey, SearchAggregation<A> aggregation) {
		factory.contribute( searchQueryBuilder.toQueryElementCollector(), aggregationKey, aggregation );
	}

	protected abstract S thisAsS();

	protected abstract PDF extendPredicateFactory(SearchPredicateFactory predicateFactory);

	protected abstract SF extendSortFactory(SearchSortFactory sortFactory);

	protected abstract AF extendAggregationFactory(SearchAggregationFactory aggregationFactory);
}
