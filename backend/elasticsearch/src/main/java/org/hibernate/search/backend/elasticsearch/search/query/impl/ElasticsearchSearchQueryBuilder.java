/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.highlighter.impl.ElasticsearchSearchHighlighter;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DistanceSortKey;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSort;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortCollector;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.QueryParameters;
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

	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private final ElasticsearchParallelWorkOrchestrator queryOrchestrator;

	private final ElasticsearchSearchIndexScope<?> scope;
	private final BackendSessionContext sessionContext;

	private final PredicateRequestContext rootPredicateContext;
	private final SearchLoadingContextBuilder<?, ?> loadingContextBuilder;
	private final ElasticsearchSearchProjection<H> rootProjection;
	private final Integer scrollTimeout;
	private final Set<String> routingKeys;
	private ElasticsearchSearchPredicate elasticsearchPredicate;
	private JsonArray jsonSort;
	private List<ElasticsearchSearchSort> elasticsearchSearchSorts;
	private Map<DistanceSortKey, Integer> distanceSorts;
	private Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations;
	private Long timeoutValue;
	private TimeUnit timeoutUnit;
	private boolean exceptionOnTimeout;
	private Long totalHitCountThreshold;
	private ElasticsearchSearchHighlighter queryHighlighter;
	private final Map<String, ElasticsearchSearchHighlighter> namedHighlighters = new HashMap<>();
	private final QueryParameters parameters = new QueryParameters();
	private ElasticsearchSearchRequestTransformer requestTransformer;

	public ElasticsearchSearchQueryBuilder(
			ElasticsearchWorkFactory workFactory,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			ElasticsearchParallelWorkOrchestrator queryOrchestrator,
			ElasticsearchSearchIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<H> rootProjection,
			Integer scrollTimeout) {
		this.workFactory = workFactory;
		this.searchResultExtractorFactory = searchResultExtractorFactory;
		this.queryOrchestrator = queryOrchestrator;

		this.scope = scope;
		this.sessionContext = sessionContext;
		this.routingKeys = new HashSet<>();

		this.rootPredicateContext = new PredicateRequestContext( sessionContext, scope, routingKeys, parameters );
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
		this.scrollTimeout = scrollTimeout;
	}

	@Override
	public void predicate(SearchPredicate predicate) {
		this.elasticsearchPredicate = ElasticsearchSearchPredicate.from( scope, predicate );
	}

	@Override
	public void sort(SearchSort sort) {
		if ( elasticsearchSearchSorts == null ) {
			elasticsearchSearchSorts = new ArrayList<>();
		}
		elasticsearchSearchSorts.add( ElasticsearchSearchSort.from( scope, sort ) );
	}

	@Override
	public <A> void aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation) {
		if ( !( aggregation instanceof ElasticsearchSearchAggregation ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherAggregations( aggregation );
		}

		ElasticsearchSearchAggregation<A> casted = (ElasticsearchSearchAggregation<A>) aggregation;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw log.aggregationDefinedOnDifferentIndexes(
					aggregation, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() )
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
	public void highlighter(SearchHighlighter queryHighlighter) {
		this.queryHighlighter = ElasticsearchSearchHighlighter.from( scope,
				queryHighlighter
		);
	}

	@Override
	public void highlighter(String highlighterName, SearchHighlighter highlighter) {
		if ( highlighterName == null || highlighterName.trim().isEmpty() ) {
			throw log.highlighterNameCannotBeBlank();
		}
		if (
			this.namedHighlighters.put(
					highlighterName,
					ElasticsearchSearchHighlighter.from( scope, highlighter )
			) != null
		) {
			throw log.highlighterWithTheSameNameCannotBeAdded( highlighterName );
		}
	}

	@Override
	public void param(String parameterName, Object value) {
		parameters.add( parameterName, value );
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

		SearchLoadingContext<?> loadingContext = loadingContextBuilder.build();

		ElasticsearchSearchQueryRequestContext requestContext = new ElasticsearchSearchQueryRequestContext(
				scope, sessionContext, loadingContext, rootPredicateContext, distanceSorts,
				namedHighlighters, queryHighlighter, parameters
		);

		JsonArray filters = rootPredicateContext.tenantAndRoutingFilters();

		JsonObject jsonPredicate = elasticsearchPredicate.toJsonQuery( rootPredicateContext );

		JsonObject jsonQuery = Queries.boolFilter( jsonPredicate, filters );
		if ( jsonQuery != null ) {
			payload.add( "query", jsonQuery );
		}

		if ( elasticsearchSearchSorts != null ) {
			for ( ElasticsearchSearchSort elasticsearchSearchSort : elasticsearchSearchSorts ) {
				elasticsearchSearchSort.toJsonSorts( this );
			}
		}

		if ( jsonSort != null ) {
			payload.add( "sort", jsonSort );
		}

		ElasticsearchSearchProjection.Extractor<?, H> rootExtractor = rootProjection.request( payload, requestContext );

		if ( aggregations != null ) {
			JsonObject jsonAggregations = new JsonObject();

			for ( Map.Entry<AggregationKey<?>, ElasticsearchSearchAggregation<?>> entry : aggregations.entrySet() ) {
				jsonAggregations.add( entry.getKey().name(), entry.getValue().request( requestContext ) );
			}

			payload.add( "aggregations", jsonAggregations );
		}

		if ( queryHighlighter != null ) {
			queryHighlighter.request( payload );
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
