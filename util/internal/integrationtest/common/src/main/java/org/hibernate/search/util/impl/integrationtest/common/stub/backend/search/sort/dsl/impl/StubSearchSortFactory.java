/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;

public class StubSearchSortFactory<SR>
		extends
		AbstractSearchSortFactory<SR, StubSearchSortFactory<SR>, SearchSortIndexScope<?>, TypedSearchPredicateFactory<SR>> {
	public StubSearchSortFactory(
			SearchSortDslContext<SR, SearchSortIndexScope<?>, TypedSearchPredicateFactory<SR>> dslContext) {
		super( dslContext );
	}

	@Override
	public StubSearchSortFactory<SR> withRoot(String objectFieldPath) {
		return new StubSearchSortFactory<>( dslContext.rescope( dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}
}
