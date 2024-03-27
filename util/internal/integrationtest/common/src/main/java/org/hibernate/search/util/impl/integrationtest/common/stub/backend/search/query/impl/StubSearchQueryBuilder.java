/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl.StubSearchAggregation;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicate;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl.StubSearchSort;

public class StubSearchQueryBuilder<H> implements SearchQueryBuilder<H> {

	private final StubBackend backend;
	private final StubSearchIndexScope scope;
	private final StubSearchWork.Builder workBuilder;
	private final StubSearchProjectionContext projectionContext;
	private final SearchLoadingContextBuilder<?, ?> loadingContextBuilder;
	private final StubSearchProjection<H> rootProjection;

	public StubSearchQueryBuilder(StubBackend backend, StubSearchIndexScope scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder, StubSearchProjection<H> rootProjection) {
		this.backend = backend;
		this.scope = scope;
		this.workBuilder = StubSearchWork.builder();
		this.projectionContext = new StubSearchProjectionContext( sessionContext );
		this.loadingContextBuilder = loadingContextBuilder;
		this.rootProjection = rootProjection;
		workBuilder.projection( rootProjection );
	}

	@Override
	public void predicate(SearchPredicate predicate) {
		// Just check the type and simulate building native constructs.
		( (StubSearchPredicate) predicate ).simulateBuild();
	}

	@Override
	public void sort(SearchSort sort) {
		// Just check the type and simulate building native constructs.
		( (StubSearchSort) sort ).simulateBuild();
	}

	@Override
	public <A> void aggregation(AggregationKey<A> key, SearchAggregation<A> aggregation) {
		// Just check the type and simulate building native constructs.
		( (StubSearchAggregation<?>) aggregation ).simulateBuild();
	}

	@Override
	public void addRoutingKey(String routingKey) {
		workBuilder.routingKey( routingKey );
	}

	@Override
	public void truncateAfter(long timeout, TimeUnit timeUnit) {
		workBuilder.truncateAfter( timeout, timeUnit );
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		workBuilder.failAfter( timeout, timeUnit );
	}

	@Override
	public void totalHitCountThreshold(long totalHitCountThreshold) {
		// totalHitCountThreshold is not tested from the mapper
	}

	@Override
	public void highlighter(SearchHighlighter queryHighlighter) {
		// highlight is not tested from the mapper
	}

	@Override
	public void highlighter(String highlighterName, SearchHighlighter highlighter) {
		// highlight is not tested from the mapper
	}

	@Override
	public SearchQuery<H> build() {
		return new StubSearchQuery<>(
				backend, scope.hibernateSearchIndexNames(), workBuilder, projectionContext,
				loadingContextBuilder.build(), rootProjection
		);
	}
}
