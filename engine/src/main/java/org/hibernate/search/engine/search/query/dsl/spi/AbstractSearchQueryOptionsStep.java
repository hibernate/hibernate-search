/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFinalStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;

public abstract class AbstractSearchQueryOptionsStep<
		SR,
		S extends SearchQueryOptionsStep<SR, S, H, LOS, SF, AF>,
		H,
		LOS,
		PDF extends TypedSearchPredicateFactory<SR>,
		SF extends TypedSearchSortFactory<SR>,
		AF extends TypedSearchAggregationFactory<SR>,
		SC extends SearchQueryIndexScope<SR, ?>>
		implements SearchQueryWhereStep<SR, S, H, LOS, PDF>, SearchQueryOptionsStep<SR, S, H, LOS, SF, AF> {

	protected final SC scope;
	private final SearchQueryBuilder<H> searchQueryBuilder;
	private final SearchLoadingContextBuilder<?, LOS> loadingContextBuilder;

	public AbstractSearchQueryOptionsStep(SC scope,
			SearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		this.scope = scope;
		this.searchQueryBuilder = searchQueryBuilder;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public S where(SearchPredicate predicate) {
		searchQueryBuilder.predicate( predicate );
		return thisAsS();
	}

	@Override
	public S where(Function<? super PDF, ? extends PredicateFinalStep> predicateContributor) {
		SearchPredicate predicate = predicateContributor.apply( predicateFactory() ).toPredicate();
		searchQueryBuilder.predicate( predicate );
		return thisAsS();
	}

	@Override
	public S where(BiConsumer<? super PDF, ? super SimpleBooleanPredicateClausesCollector<SR, ?>> predicateContributor) {
		PDF factory = predicateFactory();
		SimpleBooleanPredicateClausesStep<SR, ?> andStep = factory.and();
		predicateContributor.accept( factory, andStep );
		searchQueryBuilder.predicate( andStep.toPredicate() );
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
	public S totalHitCountThreshold(long totalHitCountThreshold) {
		searchQueryBuilder.totalHitCountThreshold( totalHitCountThreshold );
		return thisAsS();
	}

	@Override
	public S highlighter(Function<? super SearchHighlighterFactory, ? extends HighlighterFinalStep> highlighterContributor) {
		searchQueryBuilder.highlighter(
				highlighterContributor.apply(
						highlighterFactory()
				).toHighlighter() );

		return thisAsS();
	}

	@Override
	public S highlighter(SearchHighlighter highlighter) {
		searchQueryBuilder.highlighter( highlighter );

		return thisAsS();
	}

	@Override
	public S highlighter(String highlighterName,
			Function<? super SearchHighlighterFactory, ? extends HighlighterFinalStep> highlighterContributor) {
		searchQueryBuilder.highlighter(
				highlighterName,
				highlighterContributor.apply(
						highlighterFactory()
				).toHighlighter()
		);

		return thisAsS();
	}

	@Override
	public S highlighter(String highlighterName, SearchHighlighter highlighter) {
		searchQueryBuilder.highlighter( highlighterName, highlighter );

		return thisAsS();
	}

	@Override
	public S loading(Consumer<? super LOS> loadingOptionsContributor) {
		loadingOptionsContributor.accept( loadingContextBuilder.toAPI() );
		return thisAsS();
	}

	@Override
	public S sort(SearchSort sort) {
		searchQueryBuilder.sort( sort );
		return thisAsS();
	}

	@Override
	public S sort(Function<? super SF, ? extends SortFinalStep> sortContributor) {
		SearchSort sort = sortContributor.apply( sortFactory() ).toSort();
		searchQueryBuilder.sort( sort );
		return thisAsS();
	}

	@Override
	public <A> S aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation) {
		searchQueryBuilder.aggregation( key, aggregation );
		return thisAsS();
	}

	@Override
	public <A> S aggregation(AggregationKey<A> key,
			Function<? super AF, ? extends AggregationFinalStep<A>> aggregationContributor) {
		AF factory = aggregationFactory();
		SearchAggregation<A> aggregation = aggregationContributor.apply( factory ).toAggregation();
		searchQueryBuilder.aggregation( key, aggregation );
		return thisAsS();
	}

	@Override
	public S param(String parameterName, Object value) {
		searchQueryBuilder.param( parameterName, value );
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

	@Override
	public SearchScroll<H> scroll(int chunkSize) {
		return toQuery().scroll( chunkSize );
	}

	protected abstract S thisAsS();

	protected abstract PDF predicateFactory();

	protected abstract SF sortFactory();

	protected abstract AF aggregationFactory();

	protected abstract SearchHighlighterFactory highlighterFactory();

}
