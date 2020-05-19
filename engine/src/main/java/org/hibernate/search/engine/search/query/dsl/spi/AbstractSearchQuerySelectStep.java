/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.impl.DefaultSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public abstract class AbstractSearchQuerySelectStep<
				N extends SearchQueryOptionsStep<?, E, LOS, ?, ?>,
				R,
				E,
				LOS,
				PJF extends SearchProjectionFactory<R, E>,
				PDF extends SearchPredicateFactory,
				C
		>
		implements SearchQuerySelectStep<N, R, E, LOS, PJF, PDF> {

	@Override
	public <T> T extension(SearchQueryDslExtension<T, R, E, LOS> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional(
						this, indexScope(), sessionContext(), loadingContextBuilder()
				)
		);
	}

	protected final SearchProjectionFactory<R, E> createDefaultProjectionFactory() {
		return new DefaultSearchProjectionFactory<>( indexScope().searchProjectionFactory() );
	}

	protected abstract IndexScope<C> indexScope();

	protected abstract BackendSessionContext sessionContext();

	protected abstract LoadingContextBuilder<R, E, LOS> loadingContextBuilder();
}
