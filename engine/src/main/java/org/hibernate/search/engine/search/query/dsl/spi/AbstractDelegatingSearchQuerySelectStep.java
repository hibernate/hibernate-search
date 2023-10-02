/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;

public abstract class AbstractDelegatingSearchQuerySelectStep<R, E, LOS>
		implements SearchQuerySelectStep<
				SearchQueryOptionsStep<?, E, LOS, ?, ?>,
				R,
				E,
				LOS,
				SearchProjectionFactory<R, E>,
				SearchPredicateFactory> {

	private final SearchQuerySelectStep<?, R, E, LOS, ?, ?> delegate;

	public AbstractDelegatingSearchQuerySelectStep(SearchQuerySelectStep<?, R, E, LOS, ?, ?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryWhereStep<?, E, LOS, ?> selectEntity() {
		return delegate.selectEntity();
	}

	@Override
	public SearchQueryWhereStep<?, R, LOS, ?> selectEntityReference() {
		return delegate.selectEntityReference();
	}

	@Override
	public <P> SearchQueryWhereStep<?, P, LOS, ?> select(Class<P> objectClass) {
		return delegate.select( objectClass );
	}

	@Override
	public <P> SearchQueryWhereStep<?, P, LOS, ?> select(
			Function<? super SearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		return delegate.select( projectionContributor );
	}

	@Override
	public <P> SearchQueryWhereStep<?, P, LOS, ?> select(SearchProjection<P> projection) {
		return delegate.select( projection );
	}

	@Override
	public SearchQueryWhereStep<?, List<?>, LOS, ?> select(
			SearchProjection<?>... projections) {
		return delegate.select( projections );
	}

	@Override
	public SearchQueryOptionsStep<?, E, LOS, ?, ?> where(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return delegate.where( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<?, E, LOS, ?, ?> where(
			BiConsumer<? super SearchPredicateFactory,
					? super SimpleBooleanPredicateClausesCollector<?>> predicateContributor) {
		return delegate.where( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<?, E, LOS, ?, ?> where(SearchPredicate predicate) {
		return delegate.where( predicate );
	}

	@Override
	public <T> T extension(SearchQueryDslExtension<T, R, E, LOS> extension) {
		return delegate.extension( extension );
	}
}
