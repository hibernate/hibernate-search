/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.spi.HitAggregator;

import com.google.gson.JsonObject;

class ElasticsearchSearchQueryBuilderImpl<C, T> implements ElasticsearchSearchQueryBuilder<T> {

	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final ElasticsearchWorkFactory workFactory;
	private final Set<String> indexNames;
	private final Set<String> routingKeys;
	private final HitExtractor<? super C> hitExtractor;
	private final HitAggregator<C, List<T>> hitAggregator;
	private JsonObject rootQueryClause;

	public ElasticsearchSearchQueryBuilderImpl(
			ElasticsearchWorkOrchestrator queryOrchestrator,
			ElasticsearchWorkFactory workFactory,
			Set<String> indexNames,
			SessionContext context,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		this.hitExtractor = hitExtractor;
		this.hitAggregator = hitAggregator;
		String tenantId = context.getTenantIdentifier();
		if ( tenantId != null ) {
			// TODO handle tenant ID filtering
		}
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.indexNames = indexNames;
		this.routingKeys = new HashSet<>();
	}

	@Override
	public void setRootQueryClause(JsonObject rootQueryClause) {
		this.rootQueryClause = rootQueryClause;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	private SearchQuery<T> build() {
		JsonObject payload = new JsonObject();
		payload.add( "query", rootQueryClause );
		hitExtractor.contributeRequest( payload );
		SearchResultExtractor<T> searchResultExtractor =
				new SearchResultExtractorImpl<>( hitExtractor, hitAggregator );
		return new ElasticsearchSearchQuery<>( queryOrchestrator, workFactory,
				indexNames, routingKeys,
				payload, searchResultExtractor );
	}

	@Override
	public <Q> Q build(Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return searchQueryWrapperFactory.apply( build() );
	}
}
