/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.SearchQuery;

import com.google.gson.JsonObject;

class ElasticsearchSearchQueryBuilderImpl<T> implements ElasticsearchSearchQueryBuilder<T> {

	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final ElasticsearchWorkFactory workFactory;
	private final Set<String> indexNames;
	private final HitExtractor<T> hitExtractor;
	private JsonObject rootQueryClause;

	public ElasticsearchSearchQueryBuilderImpl(
			ElasticsearchWorkOrchestrator queryOrchestrator,
			ElasticsearchWorkFactory workFactory,
			Set<String> indexNames,
			SessionContext context,
			HitExtractor<T> hitExtractor) {
		String tenantId = context.getTenantIdentifier();
		if ( tenantId != null ) {
			// TODO handle tenant ID filtering
		}
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.indexNames = indexNames;
		this.hitExtractor = hitExtractor;
	}

	@Override
	public void setRootQueryClause(JsonObject rootQueryClause) {
		this.rootQueryClause = rootQueryClause;
	}

	private SearchQuery<T> build() {
		JsonObject payload = new JsonObject();
		payload.add( "query", rootQueryClause );
		hitExtractor.contributeRequest( payload );
		return new ElasticsearchSearchQuery<>( queryOrchestrator, workFactory, indexNames,
				payload, hitExtractor );
	}

	@Override
	public <Q> Q build(Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return searchQueryWrapperFactory.apply( build() );
	}
}
