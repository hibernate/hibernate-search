/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DistanceSortKey;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSort;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchSearchQueryBuilder<H>
		implements SearchQueryBuilder<H>, ElasticsearchSearchSortCollector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonElement> REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" );

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private final ElasticsearchParallelWorkOrchestrator queryOrchestrator;

	private final ElasticsearchSearchIndexScope<?> scope;
	private final BackendSessionContext sessionContext;

	private final PredicateRequestContext rootPredicateContext;
	private final SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder;
	private final ElasticsearchSearchProjection<H> rootProjection;
	private final Integer scrollTimeout;

	private final Set<String> routingKeys;
	private JsonObject jsonPredicate;
	private JsonArray jsonSort;
	private Map<DistanceSortKey, Integer> distanceSorts;
	private Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations;
	private Long timeoutValue;
	private TimeUnit timeoutUnit;
	private boolean exceptionOnTimeout;
	private Long totalHitCountThreshold;
	private ElasticsearchSearchRequestTransformer requestTransformer;

	public ElasticsearchSearchQueryBuilder(
			ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			ElasticsearchParallelWorkOrchestrator queryOrchestrator,
			ElasticsearchSearchIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<H> rootProjection,
			Integer scrollTimeout) {
		this.workFactory = workFactory;
		this.searchResultExtractorFactory = searchResultExtractorFactory;
		this.queryOrchestrator = queryOrchestrator;

		this.scope = scope;
		this.sessionContext = sessionContext;
		this.routingKeys = new HashSet<>();

		this.rootPredicateContext = new PredicateRequestContext( sessionContext );
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
		this.scrollTimeout = scrollTimeout;
	}

	@Override
	public void predicate(SearchPredicate predicate) {
		ElasticsearchSearchPredicate elasticsearchPredicate = ElasticsearchSearchPredicate.from( scope, predicate );
		this.jsonPredicate = elasticsearchPredicate.toJsonQuery( rootPredicateContext );
	}

	@Override
	public void sort(SearchSort sort) {
		ElasticsearchSearchSort elasticsearchSort = ElasticsearchSearchSort.from( scope, sort );
		elasticsearchSort.toJsonSorts( this );
	}

	@Override
	public <A> void aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation) {
		if ( !( aggregation instanceof ElasticsearchSearchAggregation ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherAggregations( aggregation );
		}

		ElasticsearchSearchAggregation<A> casted = (ElasticsearchSearchAggregation<A>) aggregation;
		if ( !scope.hibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.aggregationDefinedOnDifferentIndexes(
					aggregation, casted.getIndexNames(), scope.hibernateSearchIndexNames()
			);
		}

		if ( aggregations == null ) {
			aggregations = new LinkedHashMap<>();
		}
		Object previous = aggregations.put( key, casted );
		if ( previous != null ) {
			throw log.duplicateAggregationKey( key );
		}
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	@Override
	public void truncateAfter(long timeout, TimeUnit timeUnit) {
		// This will override any failAfter. Eventually we could allow the user to set both.
		this.timeoutValue = timeout;
		this.timeoutUnit = timeUnit;
		this.exceptionOnTimeout = false;
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		// This will override any truncateAfter. Eventually we could allow the user to set both.
		this.timeoutValue = timeout;
		this.timeoutUnit = timeUnit;
		this.exceptionOnTimeout = true;
	}

	@Override
	public void totalHitCountThreshold(long totalHitCountThreshold) {
		this.totalHitCountThreshold = totalHitCountThreshold;
	}

	@Override
	public PredicateRequestContext getRootPredicateContext() {
		return rootPredicateContext;
	}

	@Override
	public void collectSort(JsonElement sort) {
		if ( jsonSort == null ) {
			jsonSort = new JsonArray();
		}
		this.jsonSort.add( sort );
	}

	@Override
	public void collectDistanceSort(JsonElement sort, String absoluteFieldPath, GeoPoint center) {
		collectSort( sort );

		int index = jsonSort.size() - 1;
		if ( distanceSorts == null ) {
			distanceSorts = CollectionHelper.newHashMap( 3 );
		}

		distanceSorts.put( new DistanceSortKey( absoluteFieldPath, center ), index );
	}

	public void requestTransformer(ElasticsearchSearchRequestTransformer transformer) {
		Contracts.assertNotNull( transformer, "transformer" );
		this.requestTransformer = transformer;
	}

	@Override
	public ElasticsearchSearchQuery<H> build() {
		JsonObject payload = new JsonObject();

		JsonArray filters = new JsonArray();
		JsonObject filter = scope.filterOrNull( sessionContext.tenantIdentifier() );
		if ( filter != null ) {
			filters.add( filter );
		}
		if ( !routingKeys.isEmpty() ) {
			filters.add( Queries.anyTerm( "_routing", routingKeys ) );
		}
		JsonObject jsonQuery = Queries.boolFilter( jsonPredicate, filters );

		if ( jsonQuery != null ) {
			payload.add( "query", jsonQuery );
		}

		if ( jsonSort != null ) {
			payload.add( "sort", jsonSort );
		}

		SearchLoadingContext<?, ?> loadingContext = loadingContextBuilder.build();

		ElasticsearchSearchQueryRequestContext requestContext = new ElasticsearchSearchQueryRequestContext(
				scope, sessionContext, loadingContext, rootPredicateContext, distanceSorts
		);

		ElasticsearchSearchProjection.Extractor<?, H> rootExtractor = rootProjection.request( payload, requestContext );

		if ( aggregations != null ) {
			JsonObject jsonAggregations = new JsonObject();

			for ( Map.Entry<AggregationKey<?>, ElasticsearchSearchAggregation<?>> entry : aggregations.entrySet() ) {
				jsonAggregations.add( entry.getKey().name(), entry.getValue().request( requestContext ) );
			}

			payload.add( "aggregations", jsonAggregations );
		}

		if ( !REQUEST_SOURCE_ACCESSOR.get( payload ).isPresent() ) {
			REQUEST_SOURCE_ACCESSOR.set( payload, new JsonPrimitive( Boolean.FALSE ) );
		}

		TimeoutManager timeoutManager = scope.createTimeoutManager(
				timeoutValue, timeoutUnit, exceptionOnTimeout );

		ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor =
				searchResultExtractorFactory.createResultExtractor(
						requestContext,
						rootExtractor,
						aggregations == null ? Collections.emptyMap() : aggregations
				);

		return new ElasticsearchSearchQueryImpl<>(
				workFactory, queryOrchestrator,
				scope, sessionContext, loadingContext, routingKeys,
				payload, requestTransformer,
				searchResultExtractor,
				timeoutManager,
				scrollTimeout, totalHitCountThreshold
		);
	}
}
