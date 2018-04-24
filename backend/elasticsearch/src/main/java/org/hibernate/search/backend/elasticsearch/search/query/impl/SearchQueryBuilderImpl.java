/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchResultExtractor;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class SearchQueryBuilderImpl<C, T>
		implements SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> {

	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final Set<URLEncodedString> indexNames;
	private final String tenantId;
	private final Set<String> routingKeys;

	private final ElasticsearchSearchQueryElementCollector elementCollector;
	private final HitExtractor<? super C> hitExtractor;
	private final HitAggregator<C, List<T>> hitAggregator;

	SearchQueryBuilderImpl(
			ElasticsearchWorkFactory workFactory,
			ElasticsearchWorkOrchestrator queryOrchestrator,
			MultiTenancyStrategy multiTenancyStrategy,
			Set<URLEncodedString> indexNames,
			SessionContext sessionContext,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.multiTenancyStrategy = multiTenancyStrategy;

		this.indexNames = indexNames;
		this.tenantId = sessionContext.getTenantIdentifier();
		this.routingKeys = new HashSet<>();

		this.elementCollector = new ElasticsearchSearchQueryElementCollector();
		this.hitExtractor = hitExtractor;
		this.hitAggregator = hitAggregator;
	}

	@Override
	public ElasticsearchSearchQueryElementCollector getQueryElementCollector() {
		return elementCollector;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	private SearchQuery<T> build() {
		JsonObject payload = new JsonObject();

		JsonObject jsonQuery = getJsonQuery();
		if ( jsonQuery != null ) {
			payload.add( "query", jsonQuery );
		}

		JsonArray jsonSort = elementCollector.toJsonSort();
		if ( jsonSort != null ) {
			payload.add( "sort", jsonSort );
		}

		hitExtractor.contributeRequest( payload );

		SearchResultExtractor<T> searchResultExtractor =
				new SearchResultExtractorImpl<>( hitExtractor, hitAggregator );

		return new ElasticsearchSearchQuery<>(
				workFactory, queryOrchestrator,
				indexNames, routingKeys,
				payload,
				searchResultExtractor
		);
	}

	private JsonObject getJsonQuery() {
		return multiTenancyStrategy.decorateJsonQuery( elementCollector.toJsonPredicate(), tenantId );
	}

	@Override
	public <Q> Q build(Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return searchQueryWrapperFactory.apply( build() );
	}
}
