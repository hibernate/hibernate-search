/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchSearchQueryBuilder<T>
		implements SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> {

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final Set<URLEncodedString> indexNames;
	private final SessionContextImplementor sessionContext;
	private final Set<String> routingKeys;

	private final ElasticsearchSearchQueryElementCollector elementCollector;
	private final LoadingContextBuilder<?, ?> loadingContextBuilder;
	private final ElasticsearchSearchProjection<?, T> rootProjection;

	ElasticsearchSearchQueryBuilder(
			ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			ElasticsearchWorkOrchestrator queryOrchestrator,
			MultiTenancyStrategy multiTenancyStrategy,
			Set<URLEncodedString> indexNames,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<?, T> rootProjection) {
		this.workFactory = workFactory;
		this.searchResultExtractorFactory = searchResultExtractorFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.multiTenancyStrategy = multiTenancyStrategy;

		this.indexNames = indexNames;
		this.sessionContext = sessionContext;
		this.routingKeys = new HashSet<>();

		this.elementCollector = new ElasticsearchSearchQueryElementCollector( sessionContext );
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
	}

	@Override
	public ElasticsearchSearchQueryElementCollector getQueryElementCollector() {
		return elementCollector;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	private IndexSearchQuery<T> build() {
		JsonObject payload = new JsonObject();

		JsonObject jsonQuery = getJsonQuery();
		if ( jsonQuery != null ) {
			payload.add( "query", jsonQuery );
		}

		JsonArray jsonSort = elementCollector.toJsonSort();
		if ( jsonSort != null ) {
			payload.add( "sort", jsonSort );
		}

		SearchProjectionExtractContext searchProjectionExecutionContext = elementCollector
				.toSearchProjectionExecutionContext();

		rootProjection.contributeRequest( payload, searchProjectionExecutionContext );

		LoadingContext<?, ?> loadingContext = loadingContextBuilder.build();

		ElasticsearchSearchResultExtractor<T> searchResultExtractor =
				searchResultExtractorFactory.createResultExtractor(
						loadingContext,
						rootProjection, searchProjectionExecutionContext
				);

		return new ElasticsearchIndexSearchQuery<>(
				workFactory, queryOrchestrator,
				indexNames, sessionContext, loadingContext, routingKeys,
				payload,
				searchResultExtractor
		);
	}

	private JsonObject getJsonQuery() {
		return multiTenancyStrategy.decorateJsonQuery( elementCollector.toJsonPredicate(), sessionContext.getTenantIdentifier() );
	}

	@Override
	public <Q> Q build(Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory) {
		return searchQueryWrapperFactory.apply( build() );
	}
}
