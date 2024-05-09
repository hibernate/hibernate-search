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

public abstract class AbstractDelegatingSearchQuerySelectStep<SR, R, E, LOS>
		implements SearchQuerySelectStep<
				SR,
				SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?>,
				R,
				E,
				LOS,
				SearchProjectionFactory<SR, R, E>,
				SearchPredicateFactory<SR>> {

	private final SearchQuerySelectStep<SR, ?, R, E, LOS, ?, ?> delegate;

	public AbstractDelegatingSearchQuerySelectStep(SearchQuerySelectStep<SR, ?, R, E, LOS, ?, ?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryWhereStep<SR, ?, E, LOS, ?> selectEntity() {
		return delegate.selectEntity();
	}

	@Override
	public SearchQueryWhereStep<SR, ?, R, LOS, ?> selectEntityReference() {
		return delegate.selectEntityReference();
	}

	@Override
	public <P> SearchQueryWhereStep<SR, ?, P, LOS, ?> select(Class<P> objectClass) {
		return delegate.select( objectClass );
	}

	@Override
	public <P> SearchQueryWhereStep<SR, ?, P, LOS, ?> select(
			Function<? super SearchProjectionFactory<SR, R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		return delegate.select( projectionContributor );
	}

	@Override
	public <P> SearchQueryWhereStep<SR, ?, P, LOS, ?> select(SearchProjection<P> projection) {
		return delegate.select( projection );
	}

	@Override
	public SearchQueryWhereStep<SR, ?, List<?>, LOS, ?> select(
			SearchProjection<?>... projections) {
		return delegate.select( projections );
	}

	@Override
	public SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?> where(
			Function<? super SearchPredicateFactory<SR>, ? extends PredicateFinalStep> predicateContributor) {
		return delegate.where( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?> where(
			BiConsumer<? super SearchPredicateFactory<SR>,
					? super SimpleBooleanPredicateClausesCollector<SR, ?>> predicateContributor) {
		return delegate.where( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?> where(SearchPredicate predicate) {
		return delegate.where( predicate );
	}

	@Override
	public <T> T extension(SearchQueryDslExtension<SR, T, R, E, LOS> extension) {
		return delegate.extension( extension );
	}
}
