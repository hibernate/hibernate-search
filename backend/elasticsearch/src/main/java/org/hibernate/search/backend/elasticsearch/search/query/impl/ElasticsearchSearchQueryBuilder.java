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
import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
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

	private static final JsonAccessor<JsonElement> REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" );

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private final ElasticsearchParallelWorkOrchestrator queryOrchestrator;

	private final ElasticsearchSearchContext searchContext;
	private final BackendSessionContext sessionContext;

	private final ElasticsearchSearchPredicateContext rootPredicateContext;
	private final LoadingContextBuilder<?, ?, ?> loadingContextBuilder;
	private final ElasticsearchSearchProjection<?, H> rootProjection;

	private final Set<String> routingKeys;
	private JsonObject jsonPredicate;
	private JsonArray jsonSort;
	private Map<DistanceSortKey, Integer> distanceSorts;
	private Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations;
	private Long timeoutValue;
	private TimeUnit timeoutUnit;
	private boolean exceptionOnTimeout;
	private ElasticsearchSearchRequestTransformer requestTransformer;

	public ElasticsearchSearchQueryBuilder(
			ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			ElasticsearchParallelWorkOrchestrator queryOrchestrator,
			ElasticsearchSearchContext searchContext,
			BackendSessionContext sessionContext,
			LoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<?, H> rootProjection) {
		this.workFactory = workFactory;
		this.searchResultExtractorFactory = searchResultExtractorFactory;
		this.queryOrchestrator = queryOrchestrator;

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

		JsonArray filters = new JsonArray();
		JsonObject filter = searchContext.getFilterOrNull( sessionContext.getTenantIdentifier() );
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

		LoadingContext<?, ?> loadingContext = loadingContextBuilder.build();

		ElasticsearchSearchQueryRequestContext requestContext = new ElasticsearchSearchQueryRequestContext(
				searchContext, sessionContext, loadingContext, distanceSorts
		);

		rootProjection.request( payload, requestContext );

		if ( aggregations != null ) {
			JsonObject jsonAggregations = new JsonObject();

			for ( Map.Entry<AggregationKey<?>, ElasticsearchSearchAggregation<?>> entry : aggregations.entrySet() ) {
				jsonAggregations.add( entry.getKey().getName(), entry.getValue().request( requestContext ) );
			}

			payload.add( "aggregations", jsonAggregations );
		}

		if ( !REQUEST_SOURCE_ACCESSOR.get( payload ).isPresent() ) {
			REQUEST_SOURCE_ACCESSOR.set( payload, new JsonPrimitive( Boolean.FALSE ) );
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
				searchResultExtractor,
				timeoutValue, timeoutUnit, exceptionOnTimeout
		);
	}
}
