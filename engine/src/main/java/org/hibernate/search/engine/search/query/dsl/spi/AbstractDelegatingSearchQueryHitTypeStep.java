/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;

public abstract class AbstractDelegatingSearchQueryHitTypeStep<R, E, LOS>
		implements SearchQueryHitTypeStep<
						SearchQueryOptionsStep<?, E, LOS,?, ?>,
						R,
						E,
						LOS,
						SearchProjectionFactory<R, E>,
						SearchPredicateFactory
				> {

	private final SearchQueryHitTypeStep<?, R, E, LOS, ?, ?> delegate;

	public AbstractDelegatingSearchQueryHitTypeStep(SearchQueryHitTypeStep<?, R, E, LOS, ?, ?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryWhereStep<?, E, ?> asEntity() {
		return delegate.asEntity();
	}

	@Override
	public SearchQueryWhereStep<?, R, ?> asEntityReference() {
		return delegate.asEntityReference();
	}

	@Override
	public <P> SearchQueryWhereStep<?, P, ?> asProjection(
			Function<? super SearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		return delegate.asProjection( projectionContributor );
	}

	@Override
	public <P> SearchQueryWhereStep<?, P, ?> asProjection(SearchProjection<P> projection) {
		return delegate.asProjection( projection );
	}

	@Override
	public SearchQueryWhereStep<?, List<?>, ?> asProjections(
			SearchProjection<?>... projections) {
		return delegate.asProjections( projections );
	}

	@Override
	public SearchQueryOptionsStep<?, E, LOS, ?, ?> where(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
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
