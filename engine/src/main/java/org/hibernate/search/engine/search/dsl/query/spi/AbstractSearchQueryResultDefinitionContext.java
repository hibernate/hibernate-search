/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.DefaultSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContextExtension;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public abstract class AbstractSearchQueryResultDefinitionContext<R, O, PC extends SearchProjectionFactoryContext<R, O>, C>
		implements SearchQueryResultDefinitionContext<R, O, PC> {

	@Override
	public <T> T extension(SearchQueryContextExtension<T, R, O> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional(
						this, getIndexSearchScope(), getSessionContext(), getLoadingContextBuilder()
				)
		);
	}

	protected final SearchProjectionFactoryContext<R, O> createDefaultProjectionFactoryContext() {
		return new DefaultSearchProjectionFactoryContext<>( getIndexSearchScope().getSearchProjectionFactory() );
	}

	protected abstract IndexSearchScope<C> getIndexSearchScope();

	protected abstract SessionContextImplementor getSessionContext();

	protected abstract LoadingContextBuilder<R, O> getLoadingContextBuilder();
}
