/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.spi.AbstractSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

public class StubSearchAggregationFactory<SR>
		extends
		AbstractSearchAggregationFactory<SR,
				StubSearchAggregationFactory<SR>,
				SearchAggregationIndexScope<?>,
				TypedSearchPredicateFactory<SR>> {
	public StubSearchAggregationFactory(
			SearchAggregationDslContext<SR, SearchAggregationIndexScope<?>, TypedSearchPredicateFactory<SR>> dslContext) {
		super( dslContext );
	}

	@Override
	public StubSearchAggregationFactory<SR> withRoot(String objectFieldPath) {
		return new StubSearchAggregationFactory<>( dslContext.rescope( dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
	}
}
