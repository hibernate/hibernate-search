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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFinalStep;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;

public abstract class AbstractSearchQueryOptionsStep<
				S extends SearchQueryOptionsStep<S, H, LOS, SF, AF>,
				H,
				LOS,
				PDF extends SearchPredicateFactory,
				SF extends SearchSortFactory,
				AF extends SearchAggregationFactory,
				SC extends SearchQueryIndexScope<?>
		>
		implements SearchQueryWhereStep<S, H, LOS, PDF>, SearchQueryOptionsStep<S, H, LOS, SF, AF> {

	protected final SC scope;
	private final SearchQueryBuilder<H> searchQueryBuilder;
	private final SearchLoadingContextBuilder<?, ?, LOS> loadingContextBuilder;

	public AbstractSearchQueryOptionsStep(SC scope,
			SearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, ?, LOS> loadingContextBuilder) {
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
	public S where(BiConsumer<? super PDF, ? super SimpleBooleanPredicateClausesCollector<?>> predicateContributor) {
		PDF factory = predicateFactory();
		SimpleBooleanPredicateClausesStep<?> andStep = factory.and();
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
	public S highlighter(String highlighterName, Function<? super SearchHighlighterFactory, ? extends HighlighterFinalStep> highlighterContributor) {
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
	public <A> S aggregation(AggregationKey<A> key, Function<? super AF, ? extends AggregationFinalStep<A>> aggregationContributor) {
		AF factory = aggregationFactory();
		SearchAggregation<A> aggregation = aggregationContributor.apply( factory ).toAggregation();
		searchQueryBuilder.aggregation( key, aggregation );
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
