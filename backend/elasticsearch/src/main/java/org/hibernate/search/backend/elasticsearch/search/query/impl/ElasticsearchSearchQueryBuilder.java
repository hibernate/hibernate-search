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

import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DistanceSortKey;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.TimeoutStrategy;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchSearchQueryBuilder<H>
		implements SearchQueryBuilder<H, ElasticsearchSearchQueryElementCollector>,
		ElasticsearchSearchQueryElementCollector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchSearchContext searchContext;
	private final BackendSessionContext sessionContext;

	private final ElasticsearchSearchPredicateContext rootPredicateContext;
	private final LoadingContextBuilder<?, ?> loadingContextBuilder;
	private final ElasticsearchSearchProjection<?, H> rootProjection;

	private final Set<String> routingKeys;
	private JsonObject jsonPredicate;
	private JsonArray jsonSort;
	private Map<DistanceSortKey, Integer> distanceSorts;
	private Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations;
	private Long timeout;
	private TimeUnit timeUnit;
	private boolean exceptionOnTimeout;
	private ElasticsearchSearchRequestTransformer requestTransformer;

	public ElasticsearchSearchQueryBuilder(
			ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			ElasticsearchWorkOrchestrator queryOrchestrator,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchSearchContext searchContext,
			BackendSessionContext sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<?, H> rootProjection) {
		this.workFactory = workFactory;
		this.searchResultExtractorFactory = searchResultExtractorFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.multiTenancyStrategy = multiTenancyStrategy;

		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.routingKeys = new HashSet<>();

		this.rootPredicateContext = new ElasticsearchSearchPredicateContext( sessionContext );
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
	}

	@Override
	public ElasticsearchSearchQueryElementCollector toQueryElementCollector() {
		return this;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	@Override
	public void timeout(long timeout, TimeUnit timeUnit, TimeoutStrategy strategy) {
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		this.exceptionOnTimeout = ( TimeoutStrategy.RAISE_AN_EXCEPTION.equals( strategy ) );
	}

	@Override
	public ElasticsearchSearchPredicateContext getRootPredicateContext() {
		return rootPredicateContext;
	}

	@Override
	public void collectPredicate(JsonObject jsonQuery) {
		this.jsonPredicate = jsonQuery;
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

	@Override
	public <A> void collectAggregation(AggregationKey<A> key, ElasticsearchSearchAggregation<A> aggregation) {
		if ( aggregations == null ) {
			aggregations = new LinkedHashMap<>();
		}
		Object previous = aggregations.put( key, aggregation );
		if ( previous != null ) {
			throw log.duplicateAggregationKey( key );
		}
	}

	public void requestTransformer(ElasticsearchSearchRequestTransformer transformer) {
		Contracts.assertNotNull( transformer, "transformer" );
		this.requestTransformer = transformer;
	}

	@Override
	public ElasticsearchSearchQuery<H> build() {
		JsonObject payload = new JsonObject();

		JsonObject jsonQuery = multiTenancyStrategy.decorateJsonQuery(
				jsonPredicate, sessionContext.getTenantIdentifier()
		);
		if ( jsonQuery != null ) {
			payload.add( "query", jsonQuery );
		}

		if ( jsonSort != null ) {
			payload.add( "sort", jsonSort );
		}

		LoadingContext<?, ?> loadingContext = loadingContextBuilder.build();

		ElasticsearchSearchQueryRequestContext requestContext = new ElasticsearchSearchQueryRequestContext(
				sessionContext, loadingContext, distanceSorts
		);

		rootProjection.request( payload, requestContext );

		if ( aggregations != null ) {
			JsonObject jsonAggregations = new JsonObject();

			for ( Map.Entry<AggregationKey<?>, ElasticsearchSearchAggregation<?>> entry : aggregations.entrySet() ) {
				jsonAggregations.add( entry.getKey().getName(), entry.getValue().request( requestContext ) );
			}

			payload.add( "aggregations", jsonAggregations );
		}

		if ( timeout != null && timeUnit != null ) {
			payload.add( "timeout", getTimeoutString() );
		}

		ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor =
				searchResultExtractorFactory.createResultExtractor(
						requestContext,
						rootProjection,
						aggregations == null ? Collections.emptyMap() : aggregations
				);

		return new ElasticsearchSearchQueryImpl<>(
				workFactory, queryOrchestrator,
				searchContext, sessionContext, loadingContext, routingKeys,
				payload, requestTransformer,
				searchResultExtractor, exceptionOnTimeout
		);
	}

	private JsonPrimitive getTimeoutString() {
		StringBuilder builder = new StringBuilder( timeout + "" );
		switch ( timeUnit ) {
			case DAYS:
				builder.append( "d" );
				break;
			case HOURS:
				builder.append( "h" );
				break;
			case MINUTES:
				builder.append( "m" );
				break;
			case SECONDS:
				builder.append( "s" );
				break;
			case MILLISECONDS:
				builder.append( "ms" );
				break;
			case MICROSECONDS:
				builder.append( "micros" );
				break;
			case NANOSECONDS:
				builder.append( "nanos" );
				break;
		}
		return new JsonPrimitive( builder.toString() );
	}
}
