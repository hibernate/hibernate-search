/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;

public abstract class AbstractDelegatingSearchQueryResultDefinitionContext<R, E>
		implements SearchQueryResultDefinitionContext<R, E, SearchProjectionFactoryContext<R, E>> {

	private final SearchQueryResultDefinitionContext<R, E, ?> delegate;

	public AbstractDelegatingSearchQueryResultDefinitionContext(SearchQueryResultDefinitionContext<R, E, ?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryResultContext<?, E, ?> asEntity() {
		return delegate.asEntity();
	}

	@Override
	public SearchQueryResultContext<?, R, ?> asReference() {
		return delegate.asReference();
	}

	@Override
	public <P> SearchQueryResultContext<?, P, ?> asProjection(
			Function<? super SearchProjectionFactoryContext<R, E>, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		return delegate.asProjection( projectionContributor );
	}

	@Override
	public <P> SearchQueryResultContext<?, P, ?> asProjection(SearchProjection<P> projection) {
		return delegate.asProjection( projection );
	}

	@Override
	public SearchQueryResultContext<?, List<?>, ?> asProjections(
			SearchProjection<?>... projections) {
		return delegate.asProjections( projections );
	}

	@Override
	public <T> T extension(SearchQueryContextExtension<T, R, E> extension) {
		return delegate.extension( extension );
	}
}
