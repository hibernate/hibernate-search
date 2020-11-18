/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScroll;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CountWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExplainResult;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class ElasticsearchSearchQueryImpl<H> extends AbstractSearchQuery<H, ElasticsearchSearchResult<H>>
		implements ElasticsearchSearchQuery<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * ES default limit for (limit + offset); any search query beyond that limit will be rejected.
	 */
	private static final int MAX_RESULT_WINDOW_SIZE = 10000;

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchParallelWorkOrchestrator queryOrchestrator;
	private final ElasticsearchSearchContext searchContext;
	private final BackendSessionContext sessionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final Set<String> routingKeys;
	private final JsonObject payload;
	private final ElasticsearchSearchRequestTransformer requestTransformer;
	private final ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor;
	private final Integer scrollTimeout;
	private final Long totalHitCountThreshold;

	private TimeoutManager timeoutManager;

	ElasticsearchSearchQueryImpl(ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchParallelWorkOrchestrator queryOrchestrator,
			ElasticsearchSearchContext searchContext,
			BackendSessionContext sessionContext,
			LoadingContext<?, ?> loadingContext,
			Set<String> routingKeys,
			JsonObject payload,
			ElasticsearchSearchRequestTransformer requestTransformer,
			ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor,
			TimeoutManager timeoutManager, Integer scrollTimeout, Long totalHitCountThreshold) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.routingKeys = routingKeys;
		this.payload = payload;
		this.requestTransformer = requestTransformer;
		this.searchResultExtractor = searchResultExtractor;
		this.timeoutManager = timeoutManager;
		this.scrollTimeout = scrollTimeout;
		this.totalHitCountThreshold = totalHitCountThreshold;
	}

	@Override
	public String queryString() {
		return payload.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + queryString() + "]";
	}

	@Override
	public <Q> Q extension(SearchQueryExtension<Q, H> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public ElasticsearchSearchResult<H> fetch(Integer offset, Integer limit) {
		timeoutManager.start();
		NonBulkableWork<ElasticsearchLoadableSearchResult<H>> work = searchWorkBuilder()
				.paging( defaultedLimit( limit, offset ), offset )
				.totalHitCountThreshold( totalHitCountThreshold )
				.build();

		ElasticsearchSearchResultImpl<H> result = Futures.unwrappedExceptionJoin(
				queryOrchestrator.submit( work ) )
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async query execution, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking();
		timeoutManager.stop();
		return result;
	}

	@Override
	public List<H> fetchHits(Integer offset, Integer limit) {
		timeoutManager.start();
		NonBulkableWork<ElasticsearchLoadableSearchResult<H>> work = searchWorkBuilder()
				.paging( defaultedLimit( limit, offset ), offset )
				.disableTrackTotalHits()
				.build();

		ElasticsearchSearchResultImpl<H> result = Futures.unwrappedExceptionJoin(
				queryOrchestrator.submit( work ) )
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async query execution, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking();
		timeoutManager.stop();
		return result.hits();
	}

	@Override
	public long fetchTotalHitCount() {
		timeoutManager.start();

		JsonObject filteredPayload = new JsonObject();
		Optional<JsonObject> querySubTree = JsonAccessor.root().property( "query" ).asObject().get( payload );
		if ( querySubTree.isPresent() ) {
			filteredPayload.add( "query", querySubTree.get() );
		}

		CountWorkBuilder builder = workFactory.count();
		for ( ElasticsearchSearchIndexContext index : searchContext.indexes().elements() ) {
			builder.index( index.names().read() );
		}
		builder.query( filteredPayload )
				.routingKeys( routingKeys )
				// soft timeout has no meaning for a count work
				.deadline( timeoutManager.hardDeadlineOrNull() )
				.requestTransformer(
						ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( requestTransformer )
				);
		NonBulkableWork<Long> work = builder.build();
		Long result = Futures.unwrappedExceptionJoin( queryOrchestrator.submit( work ) );
		timeoutManager.stop();
		return result;
	}

	@Override
	public ElasticsearchSearchScroll<H> scroll(int chunkSize) {
		String scrollTimeoutString = this.scrollTimeout + "s";

		SearchWorkBuilder<ElasticsearchLoadableSearchResult<H>> firstScroll = searchWorkBuilder()
				.scrolling( chunkSize, scrollTimeoutString );

		return new ElasticsearchSearchScrollImpl<>( queryOrchestrator, workFactory, searchResultExtractor,
				scrollTimeoutString, firstScroll, timeoutManager );
	}

	@Override
	public JsonObject explain(Object id) {
		Contracts.assertNotNull( id, "id" );

		Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex =
				searchContext.indexes().mappedTypeNameToIndex();
		if ( mappedTypeNameToIndex.size() != 1 ) {
			throw log.explainRequiresTypeName( mappedTypeNameToIndex.keySet() );
		}

		return doExplain( mappedTypeNameToIndex.values().iterator().next(), id );
	}

	@Override
	public JsonObject explain(String typeName, Object id) {
		Contracts.assertNotNull( typeName, "typeName" );
		Contracts.assertNotNull( id, "id" );

		Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex =
				searchContext.indexes().mappedTypeNameToIndex();
		ElasticsearchSearchIndexContext index = mappedTypeNameToIndex.get( typeName );
		if ( index == null ) {
			throw log.explainRequiresTypeTargetedByQuery( mappedTypeNameToIndex.keySet(), typeName );
		}

		return doExplain( index, id );
	}

	private SearchWorkBuilder<ElasticsearchLoadableSearchResult<H>> searchWorkBuilder() {
		SearchWorkBuilder<ElasticsearchLoadableSearchResult<H>> builder =
				workFactory.search( payload, searchResultExtractor );
		for ( ElasticsearchSearchIndexContext index : searchContext.indexes().elements() ) {
			builder.index( index.names().read() );
		}
		builder
				.routingKeys( routingKeys )
				.deadline( timeoutManager.deadlineOrNull(), timeoutManager.hasHardTimeout() )
				.requestTransformer(
						ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( requestTransformer )
				);
		return builder;
	}

	private Integer defaultedLimit(Integer limit, Integer offset) {
		/*
		 * If the user has given a 'size' value, take it as is, let ES itself complain if it's too high;
		 * if no value is given, take as much as possible, as by default only 10 rows would be returned.
		 */
		if ( limit != null ) {
			return limit;
		}
		else {
			// Elasticsearch has a default limit of 10, which is not what we want.
			int maxLimitThatElasticsearchWillAccept = MAX_RESULT_WINDOW_SIZE;
			if ( offset != null ) {
				maxLimitThatElasticsearchWillAccept -= offset;
			}
			return maxLimitThatElasticsearchWillAccept;
		}
	}

	private JsonObject doExplain(ElasticsearchSearchIndexContext index, Object id) {
		JsonObject queryOnlyPayload = new JsonObject();
		JsonElement query = payload.get( "query" );
		if ( query != null ) {
			queryOnlyPayload.add( "query", query );
		}

		URLEncodedString elasticsearchId = toElasticsearchId( index, id );

		URLEncodedString indexName = index.names().read();
		NonBulkableWork<ExplainResult> work = workFactory.explain( indexName, elasticsearchId, queryOnlyPayload )
				.routingKeys( routingKeys )
				.requestTransformer(
						ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( requestTransformer )
				)
				.build();

		ExplainResult explainResult = Futures.unwrappedExceptionJoin( queryOrchestrator.submit( work ) );
		return explainResult.getJsonObject();
	}

	private URLEncodedString toElasticsearchId(ElasticsearchSearchIndexContext index, Object id) {
		ToDocumentIdentifierValueConverter<?> converter = index.idDslConverter();
		ToDocumentIdentifierValueConvertContext convertContext =
				searchContext.toDocumentIdentifierValueConvertContext();
		String documentId = converter.convertUnknown( id, convertContext );
		return URLEncodedString.fromString( searchContext.documentIdHelper()
				.toElasticsearchId( sessionContext.tenantIdentifier(), documentId ) );
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		// replace the timeout manager on already created query instance
		timeoutManager = searchContext.createTimeoutManager( timeout, timeUnit, true );
	}
}
