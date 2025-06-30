/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSyncWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContextImpl;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.backend.lucene.search.highlighter.impl.LuceneAbstractSearchHighlighter;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSort;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.QueryParameters;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

public class LuceneSearchQueryBuilder<H> implements SearchQueryBuilder<H>, LuceneSearchSortCollector {

	private final LuceneWorkFactory workFactory;
	private final LuceneSyncWorkOrchestrator queryOrchestrator;

	private final LuceneSearchQueryIndexScope<?, ?> scope;
	private final BackendSessionContext sessionContext;
	private final Set<String> routingKeys;

	private final SearchLoadingContextBuilder<?, ?> loadingContextBuilder;
	private final LuceneSearchProjection<H> rootProjection;

	private LuceneSearchPredicate lucenePredicate;
	private List<SortField> sortFields;
	private List<LuceneSearchSort> luceneSearchSorts;
	private Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations;
	private Long timeout;
	private TimeUnit timeUnit;
	private boolean exceptionOnTimeout;
	private Long totalHitCountThreshold;
	private LuceneAbstractSearchHighlighter globalHighlighter;
	private final Map<String, LuceneAbstractSearchHighlighter> namedHighlighters = new HashMap<>();
	private final QueryParameters parameters = new QueryParameters();

	public LuceneSearchQueryBuilder(
			LuceneWorkFactory workFactory,
			LuceneSyncWorkOrchestrator queryOrchestrator,
			LuceneSearchQueryIndexScope<?, ?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<H> rootProjection) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;

		this.scope = scope;
		this.sessionContext = sessionContext;
		this.routingKeys = new HashSet<>();

		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
	}

	@Override
	public void predicate(SearchPredicate predicate) {
		this.lucenePredicate = LuceneSearchPredicate.from( scope, predicate );
	}

	@Override
	public void sort(SearchSort sort) {
		if ( luceneSearchSorts == null ) {
			luceneSearchSorts = new ArrayList<>();
		}
		luceneSearchSorts.add( LuceneSearchSort.from( scope, sort ) );
	}

	@Override
	public <A> void aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation) {
		LuceneSearchAggregation<A> casted = LuceneSearchAggregation.from( scope, aggregation );

		if ( aggregations == null ) {
			aggregations = new LinkedHashMap<>();
		}
		Object previous = aggregations.put( key, casted );
		if ( previous != null ) {
			throw QueryLog.INSTANCE.duplicateAggregationKey( key );
		}
	}

	@Override
	public void addRoutingKey(String routingKey) {
		this.routingKeys.add( routingKey );
	}

	@Override
	public void truncateAfter(long timeout, TimeUnit timeUnit) {
		// This will override any failAfter. Eventually we could allow the user to set both.
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		this.exceptionOnTimeout = false;
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		// This will override any truncateAfter. Eventually we could allow the user to set both.
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		this.exceptionOnTimeout = true;
	}

	@Override
	public void totalHitCountThreshold(long totalHitCountThreshold) {
		this.totalHitCountThreshold = totalHitCountThreshold;
	}

	@Override
	public void highlighter(SearchHighlighter queryHighlighter) {
		this.globalHighlighter = LuceneAbstractSearchHighlighter.from( scope, queryHighlighter );
	}

	@Override
	public void highlighter(String highlighterName, SearchHighlighter highlighter) {
		if ( highlighterName == null || highlighterName.trim().isEmpty() ) {
			throw QueryLog.INSTANCE.highlighterNameCannotBeBlank();
		}
		if (
			this.namedHighlighters.put(
					highlighterName,
					LuceneAbstractSearchHighlighter.from( scope, highlighter )
			) != null
		) {
			throw QueryLog.INSTANCE.highlighterWithTheSameNameCannotBeAdded( highlighterName );
		}
	}

	@Override
	public void param(String parameterName, Object value) {
		parameters.add( parameterName, value );
	}

	@Override
	public void collectSortField(SortField sortField) {
		if ( sortFields == null ) {
			sortFields = new ArrayList<>( 5 );
		}
		sortFields.add( sortField );
	}

	@Override
	public void collectSortFields(SortField[] sortFields) {
		if ( sortFields == null || sortFields.length == 0 ) {
			return;
		}

		if ( this.sortFields == null ) {
			this.sortFields = new ArrayList<>( sortFields.length );
		}
		Collections.addAll( this.sortFields, sortFields );
	}

	@Override
	public PredicateRequestContext toPredicateRequestContext(String absoluteNestedPath) {
		return PredicateRequestContext.withSession( scope, sessionContext, routingKeys, parameters )
				.withNestedPath( absoluteNestedPath );
	}

	@Override
	public LuceneSearchQuery<H> build() {
		Query luceneQuery = lucenePredicate.toQuery(
				PredicateRequestContext.withSession( scope, sessionContext, routingKeys, parameters ) );

		SearchLoadingContext<?> loadingContext = loadingContextBuilder.build();

		BooleanQuery.Builder luceneQueryBuilder = new BooleanQuery.Builder();
		luceneQueryBuilder.add( luceneQuery, Occur.MUST );
		if ( scope.hasNestedDocuments() ) {
			// HSEARCH-4018: this filter has a (small) cost, so we only add it if necessary.
			luceneQueryBuilder.add( Queries.mainDocumentQuery(), Occur.FILTER );
		}
		if ( !routingKeys.isEmpty() ) {
			Query routingKeysQuery = Queries.anyTerm( MetadataFields.routingKeyFieldName(), routingKeys );
			luceneQueryBuilder.add( routingKeysQuery, Occur.FILTER );
		}

		Query filter = scope.filterOrNull( sessionContext.tenantIdentifier() );
		if ( filter != null ) {
			luceneQueryBuilder.add( filter, BooleanClause.Occur.FILTER );
		}

		Query definitiveLuceneQuery = luceneQueryBuilder.build();

		if ( luceneSearchSorts != null ) {
			for ( LuceneSearchSort luceneSearchSort : luceneSearchSorts ) {
				luceneSearchSort.toSortFields( this );
			}
		}

		Sort luceneSort = null;
		if ( sortFields != null && !sortFields.isEmpty() ) {
			luceneSort = new Sort( sortFields.toArray( new SortField[0] ) );
		}

		LuceneSearchQueryRequestContext requestContext = new LuceneSearchQueryRequestContext(
				scope, sessionContext, loadingContext, definitiveLuceneQuery, luceneSort, routingKeys, parameters
		);

		LuceneAbstractSearchHighlighter resolvedGlobalHighlighter =
				this.globalHighlighter == null ? null : this.globalHighlighter.withFallbackDefaults();
		Map<String, LuceneAbstractSearchHighlighter> resolvedNamedHighlighters = new HashMap<>();
		if ( resolvedGlobalHighlighter != null ) {
			for ( Map.Entry<String, LuceneAbstractSearchHighlighter> entry : this.namedHighlighters.entrySet() ) {
				resolvedNamedHighlighters.put(
						entry.getKey(),
						entry.getValue().withFallback( resolvedGlobalHighlighter )
				);
			}
		}
		else {
			for ( Map.Entry<String, LuceneAbstractSearchHighlighter> entry : this.namedHighlighters.entrySet() ) {
				resolvedNamedHighlighters.put(
						entry.getKey(),
						entry.getValue().withFallbackDefaults()
				);
			}
		}

		ExtractionRequirements.Builder extractionRequirementsBuilder = new ExtractionRequirements.Builder();
		ProjectionRequestContext projectionRequestContext = new ProjectionRequestContext(
				extractionRequirementsBuilder,
				resolvedGlobalHighlighter,
				resolvedNamedHighlighters,
				parameters
		);
		LuceneSearchProjection.Extractor<?, H> rootExtractor = rootProjection.request( projectionRequestContext );
		Map<AggregationKey<?>, LuceneSearchAggregation.Extractor<?>> aggregationExtractors;
		if ( aggregations != null ) {
			aggregationExtractors = new LinkedHashMap<>();
			AggregationRequestContext aggregationRequestContext =
					new AggregationRequestContextImpl( scope, sessionContext, routingKeys, extractionRequirementsBuilder,
							parameters );
			for ( Map.Entry<AggregationKey<?>, LuceneSearchAggregation<?>> entry : aggregations.entrySet() ) {
				aggregationExtractors.put( entry.getKey(), entry.getValue().request( aggregationRequestContext ) );
			}
		}
		else {
			aggregationExtractors = Collections.emptyMap();
		}
		ExtractionRequirements extractionRequirements = extractionRequirementsBuilder.build();

		TimeoutManager timeoutManager = scope.createTimeoutManager( timeout, timeUnit, exceptionOnTimeout );

		LuceneSearcherImpl<H> searcher = new LuceneSearcherImpl<>(
				requestContext,
				rootExtractor,
				aggregationExtractors,
				extractionRequirements,
				timeoutManager
		);

		return new LuceneSearchQueryImpl<>(
				queryOrchestrator, workFactory,
				scope,
				sessionContext,
				loadingContext,
				routingKeys,
				timeoutManager,
				definitiveLuceneQuery,
				luceneSort,
				searcher, totalHitCountThreshold
		);
	}
}
