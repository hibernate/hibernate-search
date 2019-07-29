/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchSearchQueryBuilder<H>
		implements SearchQueryBuilder<H, ElasticsearchSearchQueryElementCollector>,
				ElasticsearchSearchQueryElementCollector {

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchSearchContext searchContext;
	private final SessionContextImplementor sessionContext;

	private final ElasticsearchSearchPredicateContext rootPredicateContext;
	private final LoadingContextBuilder<?, ?> loadingContextBuilder;
	private final ElasticsearchSearchProjection<?, H> rootProjection;

	private final Set<String> routingKeys;
	private JsonObject jsonPredicate;
	private JsonArray jsonSort;
	private Map<SearchProjectionExtractContext.DistanceSortKey, Integer> distanceSorts;

	public ElasticsearchSearchQueryBuilder(
			ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			ElasticsearchWorkOrchestrator queryOrchestrator,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchSearchContext searchContext,
			SessionContextImplementor sessionContext,
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

		distanceSorts.put( new SearchProjectionExtractContext.DistanceSortKey( absoluteFieldPath, center ), index );
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

		SearchProjectionExtractContext searchProjectionExecutionContext =
				new SearchProjectionExtractContext( distanceSorts );

		rootProjection.contributeRequest( payload, searchProjectionExecutionContext );

		LoadingContext<?, ?> loadingContext = loadingContextBuilder.build();

		ElasticsearchSearchQueryRequestContext requestContext = new ElasticsearchSearchQueryRequestContext(
				sessionContext, loadingContext, distanceSorts
		);

		ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor =
				searchResultExtractorFactory.createResultExtractor(
						requestContext,
						rootProjection
				);

		return new ElasticsearchSearchQueryImpl<>(
				workFactory, queryOrchestrator,
				searchContext, sessionContext, loadingContext, routingKeys,
				payload,
				searchResultExtractor
		);
	}

}
