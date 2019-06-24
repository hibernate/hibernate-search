/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.projection.impl.DefaultSearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public abstract class AbstractSearchQueryResultDefinitionContext<
				N extends SearchQueryContext<?, E, ?>,
				R,
				E,
				PJF extends SearchProjectionFactory<R, E>,
				PDF extends SearchPredicateFactory,
				C
		>
		implements SearchQueryResultDefinitionContext<N, R, E, PJF, PDF> {

	@Override
	public <T> T extension(SearchQueryContextExtension<T, R, E> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional(
						this, getIndexScope(), getSessionContext(), getLoadingContextBuilder()
				)
		);
	}

	protected final SearchProjectionFactory<R, E> createDefaultProjectionFactory() {
		return new DefaultSearchProjectionFactory<>( getIndexScope().getSearchProjectionFactory() );
	}

	protected abstract IndexScope<C> getIndexScope();

	protected abstract SessionContextImplementor getSessionContext();

	protected abstract LoadingContextBuilder<R, E> getLoadingContextBuilder();
}
