/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;

/**
 * A wrapper around {@link MappedIndexScope} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class GenericStubMappingScope<SR, R, E> {

	private final StubMapping mapping;
	private final MappedIndexScope<SR, R, E> delegate;
	private final SearchLoadingContext<E> loadingContext;

	GenericStubMappingScope(StubMapping mapping, MappedIndexScope<SR, R, E> delegate,
			SearchLoadingContext<E> loadingContext) {
		this.mapping = mapping;
		this.delegate = delegate;
		this.loadingContext = loadingContext;
	}

	public SearchQuerySelectStep<SR, ?, R, E, StubLoadingOptionsStep, ?, ?> query() {
		return query( mapping.session() );
	}

	public SearchQuerySelectStep<SR, ?, R, E, StubLoadingOptionsStep, ?, ?> query(StubSession sessionContext) {
		return query( sessionContext, new StubLoadingOptionsStep() );
	}

	public <LOS> SearchQuerySelectStep<SR, ?, R, E, LOS, ?, ?> query(LOS loadingOptionsStep) {
		return query( mapping.session(), loadingOptionsStep );
	}

	public <LOS> SearchQuerySelectStep<SR, ?, R, E, LOS, ?, ?> query(StubSession sessionContext,
			LOS loadingOptionsStep) {
		SearchLoadingContextBuilder<E, LOS> loadingContextBuilder = new SearchLoadingContextBuilder<E, LOS>() {
			@Override
			public LOS toAPI() {
				return loadingOptionsStep;
			}

			@Override
			public SearchLoadingContext<E> build() {
				return loadingContext;
			}
		};
		return delegate.search( sessionContext, loadingContextBuilder );
	}

	public TypedSearchPredicateFactory<SR> predicate() {
		return delegate.predicate();
	}

	public TypedSearchSortFactory<SR> sort() {
		return delegate.sort();
	}

	public TypedSearchProjectionFactory<SR, R, E> projection() {
		return delegate.projection();
	}

	public TypedSearchAggregationFactory<SR> aggregation() {
		return delegate.aggregation();
	}

	public SearchHighlighterFactory highlighter() {
		return delegate.highlighter();
	}

	public <T> T extension(IndexScopeExtension<T> extension) {
		return delegate.extension( extension );
	}
}
