/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;

public class StubSearchPredicateFactory<SR>
		extends AbstractSearchPredicateFactory<SR, StubSearchPredicateFactory<SR>, SearchPredicateIndexScope<?>> {
	public StubSearchPredicateFactory(Class<SR> rootScopeType,
			SearchPredicateDslContext<SearchPredicateIndexScope<?>> dslContext) {
		super( rootScopeType, dslContext );
	}

	@Override
	public StubSearchPredicateFactory<SR> withRoot(String objectFieldPath) {
		return new StubSearchPredicateFactory<>( scopeRootType,
				dslContext.rescope( dslContext.scope().withRoot( objectFieldPath ) ) );
	}
}
