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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.impl.DefaultSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.impl.SearchAggregationDslContextImpl;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.impl.DefaultSearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
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
				S extends SearchQueryOptionsStep<S, H, LOS, SF, AF>,
				H,
				LOS,
				PDF extends SearchPredicateFactory,
				SF extends SearchSortFactory,
				AF extends SearchAggregationFactory,
				C
		>
		implements SearchQueryWhereStep<S, H, PDF>, SearchQueryOptionsStep<S, H, LOS, SF, AF> {

	private final IndexScope<C> indexScope;
	private final SearchQueryBuilder<H, C> searchQueryBuilder;
	private final LoadingContextBuilder<?, ?, LOS> loadingContextBuilder;

	public AbstractSearchQueryOptionsStep(IndexScope<C> indexScope,
			SearchQueryBuilder<H, C> searchQueryBuilder,
			LoadingContextBuilder<?, ?, LOS> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.searchQueryBuilder = searchQueryBuilder;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public S where(SearchPredicate predicate) {
		SearchPredicateBuilderFactory<? super C> factory = indexScope.searchPredicateBuilderFactory();
		contribute( factory, predicate );
		return thisAsS();
	}

	@Override
	public S where(Function<? super PDF, ? extends PredicateFinalStep> predicateContributor) {
		SearchPredicateBuilderFactory<? super C> builderFactory = indexScope.searchPredicateBuilderFactory();
		SearchPredicateFactory factory = new DefaultSearchPredicateFactory( builderFactory );
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
	public S truncateAfter(long timeout, TimeUnit timeUnit) {
		searchQueryBuilder.truncateAfter( timeout, timeUnit );
		return thisAsS();
	}

	@Override
	public S failAfter(long timeout, TimeUnit timeUnit) {
		searchQueryBuilder.failAfter( timeout, timeUnit );
		return thisAsS();
	}

	@Override
	public S loading(Consumer<? super LOS> loadingOptionsContributor) {
		loadingOptionsContributor.accept( loadingContextBuilder.toAPI() );
		return thisAsS();
	}

	@Override
	public S sort(SearchSort sort) {
		SearchSortBuilderFactory<? super C> factory = indexScope.searchSortBuilderFactory();
		contribute( factory, sort );
		return thisAsS();
	}

	@Override
	public S sort(Function<? super SF, ? extends SortFinalStep> sortContributor) {
		SearchSortBuilderFactory<? super C> builderFactory = indexScope.searchSortBuilderFactory();
		SearchPredicateBuilderFactory<? super C> predicateBuilderFactory = indexScope.searchPredicateBuilderFactory();
		SearchPredicateFactory predicateFactory = new DefaultSearchPredicateFactory( predicateBuilderFactory );
		SearchSortFactory factory = new DefaultSearchSortFactory(
				SearchSortDslContextImpl.root( builderFactory, predicateFactory )
		);
		SearchSort sort = sortContributor.apply( extendSortFactory( factory ) ).toSort();
		contribute( builderFactory, sort );
		return thisAsS();
	}

	@Override
	public <A> S aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation) {
		SearchAggregationBuilderFactory<? super C> builderFactory = indexScope.searchAggregationFactory();
		contribute( builderFactory, key, aggregation );
		return thisAsS();
	}

	@Override
	public <A> S aggregation(AggregationKey<A> key, Function<? super AF, ? extends AggregationFinalStep<A>> aggregationContributor) {
		SearchAggregationBuilderFactory<? super C> builderFactory = indexScope.searchAggregationFactory();
		SearchPredicateBuilderFactory<? super C> predicateBuilderFactory = indexScope.searchPredicateBuilderFactory();
		SearchPredicateFactory predicateFactory = new DefaultSearchPredicateFactory( predicateBuilderFactory );
		AF factory = extendAggregationFactory( new DefaultSearchAggregationFactory(
				SearchAggregationDslContextImpl.root( builderFactory, predicateFactory )
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
	public SearchResult<H> fetchAll() {
		return toQuery().fetchAll();
	}

	@Override
	public SearchResult<H> fetch(Integer limit) {
		return toQuery().fetch( limit );
	}

	@Override
	public SearchResult<H> fetch(Integer offset, Integer limit) {
		return toQuery().fetch( offset, limit );
	}

	@Override
	public List<H> fetchAllHits() {
		return toQuery().fetchAllHits();
	}

	@Override
	public List<H> fetchHits(Integer limit) {
		return toQuery().fetchHits( limit );
	}

	@Override
	public List<H> fetchHits(Integer offset, Integer limit) {
		return toQuery().fetchHits( offset, limit );
	}

	@Override
	public Optional<H> fetchSingleHit() {
		return toQuery().fetchSingleHit();
	}

	@Override
	public long fetchTotalHitCount() {
		return toQuery().fetchTotalHitCount();
	}

	private void contribute(SearchPredicateBuilderFactory<? super C> factory, SearchPredicate predicate) {
		factory.contribute( searchQueryBuilder.toQueryElementCollector(), predicate );
	}

	private void contribute(SearchSortBuilderFactory<? super C> factory, SearchSort sort) {
		factory.contribute( searchQueryBuilder.toQueryElementCollector(), sort );
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
