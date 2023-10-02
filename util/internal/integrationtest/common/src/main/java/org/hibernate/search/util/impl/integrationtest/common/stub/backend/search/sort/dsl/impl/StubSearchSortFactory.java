/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;

public class StubSearchSortFactory
		extends AbstractSearchSortFactory<StubSearchSortFactory, SearchSortIndexScope<?>, SearchPredicateFactory> {
	public StubSearchSortFactory(SearchSortDslContext<SearchSortIndexScope<?>, SearchPredicateFactory> dslContext) {
		super( dslContext );
	}

	@Override
	public StubSearchSortFactory withRoot(String objectFieldPath) {
		return new StubSearchSortFactory( dslContext.rescope( dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}
}
