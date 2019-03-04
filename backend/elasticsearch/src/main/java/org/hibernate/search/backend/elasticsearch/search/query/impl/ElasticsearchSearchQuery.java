/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.spi.SearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchResult;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchSearchQuery<T> implements SearchQuery<T> {

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final Set<URLEncodedString> indexNames;
	private final SessionContextImplementor sessionContext;
	private final Set<String> routingKeys;
	private final JsonObject payload;
	private final ElasticsearchSearchResultExtractor<T> searchResultExtractor;

	private Long firstResultIndex;
	private Long maxResultsCount;

	public ElasticsearchSearchQuery(ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchWorkOrchestrator queryOrchestrator,
			Set<URLEncodedString> indexNames,
			SessionContextImplementor sessionContext,
			Set<String> routingKeys,
			JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.indexNames = indexNames;
		this.sessionContext = sessionContext;
		this.routingKeys = routingKeys;
		this.payload = payload;
		this.searchResultExtractor = searchResultExtractor;
	}

	@Override
	public void setFirstResult(Long firstResultIndex) {
		this.firstResultIndex = firstResultIndex;
	}

	@Override
	public void setMaxResults(Long maxResultsCount) {
		this.maxResultsCount = maxResultsCount;
	}

	@Override
	public String getQueryString() {
		return payload.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getQueryString() + "]";
	}

	@Override
	public SearchResult<T> execute() {
		// TODO restore scrolling support. See HSEARCH-3323
		ElasticsearchWork<ElasticsearchLoadableSearchResult<T>> work = workFactory.search( payload, searchResultExtractor )
				.indexes( indexNames )
				.paging( firstResultIndex, maxResultsCount )
				.routingKeys( routingKeys ).build();

		return queryOrchestrator.submit( work ).join()
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async query execution, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and ObjectLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking( sessionContext );
	}

	@Override
	public long executeCount() {
		JsonObject filteredPayload = new JsonObject();
		Optional<JsonObject> querySubTree = JsonAccessor.root().property( "query" ).asObject().get( payload );
		if ( querySubTree.isPresent() ) {
			filteredPayload.add( "query", querySubTree.get() );
		}

		ElasticsearchWork<Long> work = workFactory.count( indexNames ).query( filteredPayload ).routingKeys( routingKeys ).build();
		return queryOrchestrator.submit( work ).join();
	}
}
