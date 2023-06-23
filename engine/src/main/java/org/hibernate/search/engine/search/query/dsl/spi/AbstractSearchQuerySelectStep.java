/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;

public abstract class AbstractSearchQuerySelectStep<
		N extends SearchQueryOptionsStep<?, E, LOS, ?, ?>,
		R,
		E,
		LOS,
		PJF extends SearchProjectionFactory<R, E>,
		PDF extends SearchPredicateFactory>
		implements SearchQuerySelectStep<N, R, E, LOS, PJF, PDF> {

	@Override
	public <T> T extension(SearchQueryDslExtension<T, R, E, LOS> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional( this, scope(), sessionContext(), loadingContextBuilder() )
		);
	}

	protected abstract SearchQueryIndexScope<?> scope();

	protected abstract BackendSessionContext sessionContext();

	protected abstract SearchLoadingContextBuilder<E, LOS> loadingContextBuilder();
}
