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
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.query.spi.IndexSearchQueryExtension;
import org.hibernate.search.engine.search.query.spi.IndexSearchResult;
import org.hibernate.search.util.common.impl.Futures;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSearchQuery<T> implements IndexSearchQuery<T> {

	/**
	 * ES default limit for (limit + offset); any search query beyond that limit will be rejected.
	 */
	private static final long MAX_RESULT_WINDOW_SIZE = 10000;

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;
	private final Set<URLEncodedString> indexNames;
	private final SessionContextImplementor sessionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final Set<String> routingKeys;
	private final JsonObject payload;
	private final ElasticsearchSearchResultExtractor<T> searchResultExtractor;

	public ElasticsearchIndexSearchQuery(ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchWorkOrchestrator queryOrchestrator,
			Set<URLEncodedString> indexNames,
			SessionContextImplementor sessionContext,
			LoadingContext<?, ?> loadingContext,
			Set<String> routingKeys,
			JsonObject payload, ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.indexNames = indexNames;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.routingKeys = routingKeys;
		this.payload = payload;
		this.searchResultExtractor = searchResultExtractor;
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
	public <Q> Q extension(IndexSearchQueryExtension<Q, T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public IndexSearchResult<T> fetch(Long limit, Long offset) {
		// TODO restore scrolling support. See HSEARCH-3323
		ElasticsearchWork<ElasticsearchLoadableSearchResult<T>> work = workFactory.search( payload, searchResultExtractor )
				.indexes( indexNames )
				.paging( defaultedLimit( limit, offset ), offset )
				.routingKeys( routingKeys ).build();

		return Futures.unwrappedExceptionJoin( queryOrchestrator.submit( work ) )
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
	public long fetchTotalHitCount() {
		JsonObject filteredPayload = new JsonObject();
		Optional<JsonObject> querySubTree = JsonAccessor.root().property( "query" ).asObject().get( payload );
		if ( querySubTree.isPresent() ) {
			filteredPayload.add( "query", querySubTree.get() );
		}

		ElasticsearchWork<Long> work = workFactory.count( indexNames ).query( filteredPayload ).routingKeys( routingKeys ).build();
		return queryOrchestrator.submit( work ).join();
	}

	private Long defaultedLimit(Long limit, Long offset) {
		/*
		 * If the user has given a 'size' value, take it as is, let ES itself complain if it's too high;
		 * if no value is given, take as much as possible, as by default only 10 rows would be returned.
		 */
		if ( limit != null ) {
			return limit;
		}
		else {
			// Elasticsearch has a default limit of 10, which is not what we want.
			long maxLimitThatElasticsearchWillAccept = MAX_RESULT_WINDOW_SIZE;
			if ( offset != null ) {
				maxLimitThatElasticsearchWillAccept -= offset;
			}
			return maxLimitThatElasticsearchWillAccept;
		}
	}
}
