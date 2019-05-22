/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.DefaultSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public abstract class AbstractSearchQueryResultDefinitionContext<
				N extends SearchQueryContext<?, E, ?>,
				R,
				E,
				PJC extends SearchProjectionFactoryContext<R, E>,
				PDC extends SearchPredicateFactoryContext,
				C
		>
		implements SearchQueryResultDefinitionContext<N, R, E, PJC, PDC> {

	@Override
	public <T> T extension(SearchQueryContextExtension<T, R, E> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional(
						this, getIndexSearchScope(), getSessionContext(), getLoadingContextBuilder()
				)
		);
	}

	protected final SearchProjectionFactoryContext<R, E> createDefaultProjectionFactoryContext() {
		return new DefaultSearchProjectionFactoryContext<>( getIndexSearchScope().getSearchProjectionFactory() );
	}

	protected abstract IndexSearchScope<C> getIndexSearchScope();

	protected abstract SessionContextImplementor getSessionContext();

	protected abstract LoadingContextBuilder<R, E> getLoadingContextBuilder();
}
