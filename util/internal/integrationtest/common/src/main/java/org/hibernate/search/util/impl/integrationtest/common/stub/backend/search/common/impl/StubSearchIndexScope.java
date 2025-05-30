/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.AbstractSearchIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.dsl.impl.StubSearchAggregationFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl.StubSearchAggregationBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.dsl.impl.StubSearchPredicateFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicateBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.dsl.impl.StubSearchProjectionFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchQueryBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.dsl.impl.StubSearchSortFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl.StubSearchSortBuilderFactory;

public class StubSearchIndexScope<SR>
		extends AbstractSearchIndexScope<
				SR,
				StubSearchIndexScope<SR>,
				StubIndexModel,
				StubSearchIndexNodeContext,
				StubSearchIndexCompositeNodeContext> {
	private final StubBackend backend;
	private final StubSearchPredicateBuilderFactory predicateFactory;
	private final StubSearchSortBuilderFactory sortFactory;
	private final StubSearchProjectionBuilderFactory projectionFactory;
	private final StubSearchAggregationBuilderFactory aggregationFactory;

	public StubSearchIndexScope(BackendMappingContext mappingContext, Class<SR> rootScopeType, StubBackend backend,
			Set<StubIndexModel> indexModels) {
		super( mappingContext, rootScopeType, indexModels );
		this.backend = backend;
		this.predicateFactory = new StubSearchPredicateBuilderFactory();
		this.sortFactory = new StubSearchSortBuilderFactory();
		this.projectionFactory = new StubSearchProjectionBuilderFactory( this );
		this.aggregationFactory = new StubSearchAggregationBuilderFactory();
	}

	private StubSearchIndexScope(StubSearchIndexScope<SR> parentScope, StubSearchIndexCompositeNodeContext overriddenRoot) {
		super( parentScope, overriddenRoot );
		this.backend = parentScope.backend;
		this.predicateFactory = new StubSearchPredicateBuilderFactory();
		this.sortFactory = new StubSearchSortBuilderFactory();
		this.projectionFactory = new StubSearchProjectionBuilderFactory( this );
		this.aggregationFactory = new StubSearchAggregationBuilderFactory();
	}

	@Override
	protected StubSearchIndexScope<SR> self() {
		return this;
	}

	@Override
	public StubSearchIndexScope<SR> withRoot(String objectFieldPath) {
		return new StubSearchIndexScope<>( this, field( objectFieldPath ).toComposite() );
	}

	@Override
	public SearchPredicateBuilderFactory predicateBuilders() {
		return predicateFactory;
	}

	@Override
	public SearchProjectionBuilderFactory projectionBuilders() {
		return projectionFactory;
	}

	@Override
	public SearchSortBuilderFactory sortBuilders() {
		return sortFactory;
	}

	@Override
	public SearchAggregationBuilderFactory aggregationBuilders() {
		return aggregationFactory;
	}

	@Override
	public <P> SearchQueryBuilder<P> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder, SearchProjection<P> projection) {
		return new StubSearchQueryBuilder<>( backend, this, sessionContext, loadingContextBuilder,
				(StubSearchProjection<P>) projection );
	}

	@Override
	public TypedSearchPredicateFactory<SR> predicateFactory() {
		return new StubSearchPredicateFactory<>( rootScopeType, SearchPredicateDslContext.root( this ) );
	}

	@Override
	public TypedSearchSortFactory<SR> sortFactory() {
		return new StubSearchSortFactory<>( SearchSortDslContext.root( this, StubSearchSortFactory::new, predicateFactory() ) );
	}

	@Override
	public <R, E> TypedSearchProjectionFactory<SR, R, E> projectionFactory() {
		return new StubSearchProjectionFactory<>( SearchProjectionDslContext.root( this ) );
	}

	@Override
	public TypedSearchAggregationFactory<SR> aggregationFactory() {
		return new StubSearchAggregationFactory<>( SearchAggregationDslContext.root( this, predicateFactory() ) );
	}

	@Override
	public SearchHighlighterFactory highlighterFactory() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected StubSearchIndexCompositeNodeContext createMultiIndexSearchRootContext(
			List<StubSearchIndexCompositeNodeContext> rootForEachIndex) {
		return new StubMultiIndexSearchIndexCompositeNodeContext( this, null,
				rootForEachIndex );
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected StubSearchIndexNodeContext createMultiIndexSearchValueFieldContext(String absolutePath,
			List<StubSearchIndexNodeContext> fieldForEachIndex) {
		return new StubMultiIndexSearchIndexValueFieldContext<>( this, absolutePath,
				(List) fieldForEachIndex );
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected StubSearchIndexNodeContext createMultiIndexSearchObjectFieldContext(String absolutePath,
			List<StubSearchIndexNodeContext> fieldForEachIndex) {
		return new StubMultiIndexSearchIndexCompositeNodeContext( this, absolutePath,
				(List) fieldForEachIndex );
	}
}
