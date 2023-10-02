/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScroll;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.CountWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExplainResult;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
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

	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchParallelWorkOrchestrator queryOrchestrator;
	private final ElasticsearchSearchIndexScope<?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final Set<String> routingKeys;
	private final JsonObject payload;
	private final ElasticsearchSearchRequestTransformer requestTransformer;
	private final ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor;
	private final Integer scrollTimeout;
	private final Long totalHitCountThreshold;

	/**
	 * ES limit for (limit + offset); any search query beyond that limit will be rejected.
	 */
	private final int maxResultWindow;

	private TimeoutManager timeoutManager;

	ElasticsearchSearchQueryImpl(ElasticsearchWorkFactory workFactory,
			ElasticsearchParallelWorkOrchestrator queryOrchestrator,
			ElasticsearchSearchIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContext<?> loadingContext,
			Set<String> routingKeys,
			JsonObject payload,
			ElasticsearchSearchRequestTransformer requestTransformer,
			ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor,
			TimeoutManager timeoutManager, Integer scrollTimeout, Long totalHitCountThreshold) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.routingKeys = routingKeys;
		this.payload = payload;
		this.requestTransformer = requestTransformer;
		this.searchResultExtractor = searchResultExtractor;
		this.timeoutManager = timeoutManager;
		this.scrollTimeout = scrollTimeout;
		this.totalHitCountThreshold = totalHitCountThreshold;
		this.maxResultWindow = scope.maxResultWindow();
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
		Integer defaultedLimit = defaultedLimit( limit, offset );
		NonBulkableWork<ElasticsearchLoadableSearchResult<H>> work = searchWorkBuilder()
				.paging( defaultedLimit, offset )
				.totalHitCountThreshold( totalHitCountThreshold )
				.build();

		ElasticsearchSearchResultImpl<H> result = Futures.unwrappedExceptionJoin(
				queryOrchestrator.submit( work, OperationSubmitter.blocking() ) )
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async query execution, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking();
		timeoutManager.stop();

		if ( limit == null && result.total().hitCountLowerBound() > defaultedLimit ) {
			// user may not be aware of this defaultedLimit
			log.defaultedLimitedHits( defaultedLimit, result.total().hitCountLowerBound() );
		}
		return result;
	}

	@Override
	public List<H> fetchHits(Integer offset, Integer limit) {
		timeoutManager.start();
		Integer defaultedLimit = defaultedLimit( limit, offset );
		NonBulkableWork<ElasticsearchLoadableSearchResult<H>> work = searchWorkBuilder()
				.paging( defaultedLimit, offset )
				.disableTrackTotalHits()
				.build();

		ElasticsearchSearchResultImpl<H> result = Futures.unwrappedExceptionJoin(
				queryOrchestrator.submit( work, OperationSubmitter.blocking() ) )
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async query execution, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking();
		timeoutManager.stop();

		if ( limit == null && result.total().hitCountLowerBound() > defaultedLimit ) {
			// user may not be aware of this defaultedLimit
			log.defaultedLimitedHits( defaultedLimit, result.total().hitCountLowerBound() );
		}
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

		CountWork.Builder builder = workFactory.count();
		for ( ElasticsearchSearchIndexContext index : scope.indexes() ) {
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
		Long result = Futures.unwrappedExceptionJoin( queryOrchestrator.submit( work, OperationSubmitter.blocking() ) );
		timeoutManager.stop();
		return result;
	}

	@Override
	public ElasticsearchSearchScroll<H> scroll(int chunkSize) {
		String scrollTimeoutString = this.scrollTimeout + "s";

		SearchWork.Builder<ElasticsearchLoadableSearchResult<H>> firstScroll = searchWorkBuilder()
				.scrolling( chunkSize, scrollTimeoutString );

		return new ElasticsearchSearchScrollImpl<>( queryOrchestrator, workFactory, searchResultExtractor,
				scrollTimeoutString, firstScroll, timeoutManager );
	}

	@Override
	public JsonObject explain(Object id) {
		Contracts.assertNotNull( id, "id" );

		Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex =
				scope.mappedTypeNameToIndex();
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
				scope.mappedTypeNameToIndex();
		ElasticsearchSearchIndexContext index = mappedTypeNameToIndex.get( typeName );
		if ( index == null ) {
			throw log.explainRequiresTypeTargetedByQuery( mappedTypeNameToIndex.keySet(), typeName );
		}

		return doExplain( index, id );
	}

	private SearchWork.Builder<ElasticsearchLoadableSearchResult<H>> searchWorkBuilder() {
		SearchWork.Builder<ElasticsearchLoadableSearchResult<H>> builder =
				workFactory.search( payload, searchResultExtractor );
		for ( ElasticsearchSearchIndexContext index : scope.indexes() ) {
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
			int maxLimitThatElasticsearchWillAccept = maxResultWindow;
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

		ExplainResult explainResult =
				Futures.unwrappedExceptionJoin( queryOrchestrator.submit( work, OperationSubmitter.blocking() ) );
		return explainResult.getJsonObject();
	}

	private URLEncodedString toElasticsearchId(ElasticsearchSearchIndexContext index, Object id) {
		DslConverter<?, String> converter = index.identifier().dslConverter();
		ToDocumentValueConvertContext convertContext = scope.toDocumentValueConvertContext();
		String documentId = converter.unknownTypeToDocumentValue( id, convertContext );
		return URLEncodedString.fromString( scope.documentIdHelper()
				.toElasticsearchId( sessionContext.tenantIdentifier(), documentId ) );
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		// replace the timeout manager on already created query instance
		timeoutManager = scope.createTimeoutManager( timeout, timeUnit, true );
	}
}
