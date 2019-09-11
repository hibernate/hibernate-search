/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryPredicateStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;

public abstract class AbstractDelegatingSearchQueryHitTypeStep<R, E>
		implements SearchQueryHitTypeStep<
		SearchQueryOptionsStep<?, E, ?, ?>,
								R,
								E,
								SearchProjectionFactory<R, E>,
								SearchPredicateFactory
						> {

	private final SearchQueryHitTypeStep<?, R, E, ?, ?> delegate;

	public AbstractDelegatingSearchQueryHitTypeStep(SearchQueryHitTypeStep<?, R, E, ?, ?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryPredicateStep<?, E, ?> asEntity() {
		return delegate.asEntity();
	}

	@Override
	public SearchQueryPredicateStep<?, R, ?> asEntityReference() {
		return delegate.asEntityReference();
	}

	@Override
	public <P> SearchQueryPredicateStep<?, P, ?> asProjection(
			Function<? super SearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		return delegate.asProjection( projectionContributor );
	}

	@Override
	public <P> SearchQueryPredicateStep<?, P, ?> asProjection(SearchProjection<P> projection) {
		return delegate.asProjection( projection );
	}

	@Override
	public SearchQueryPredicateStep<?, List<?>, ?> asProjections(
			SearchProjection<?>... projections) {
		return delegate.asProjections( projections );
	}

	@Override
	public SearchQueryOptionsStep<?, E, ?, ?> predicate(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return delegate.predicate( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<?, E, ?, ?> predicate(SearchPredicate predicate) {
		return delegate.predicate( predicate );
	}

	@Override
	public <T> T extension(SearchQueryDslExtension<T, R, E> extension) {
		return delegate.extension( extension );
	}
}
