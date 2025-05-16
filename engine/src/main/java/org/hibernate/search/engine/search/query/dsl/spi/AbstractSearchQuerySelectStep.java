/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;

public abstract class AbstractSearchQuerySelectStep<
		SR,
		N extends SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?>,
		R,
		E,
		LOS,
		PJF extends TypedSearchProjectionFactory<SR, R, E>,
		PDF extends TypedSearchPredicateFactory<SR>>
		implements SearchQuerySelectStep<SR, N, R, E, LOS, PJF, PDF> {

	@Override
	public <T> T extension(SearchQueryDslExtension<SR, T, R, E, LOS> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional( this, scope(), sessionContext(), loadingContextBuilder() )
		);
	}

	protected abstract SearchQueryIndexScope<?> scope();

	protected abstract BackendSessionContext sessionContext();

	protected abstract SearchLoadingContextBuilder<E, LOS> loadingContextBuilder();
}
