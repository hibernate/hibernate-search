/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class QueryStringPredicateFieldStepImpl
		implements QueryStringPredicateFieldStep<QueryStringPredicateFieldMoreStep<?, ?>> {

	private final QueryStringPredicateFieldMoreStepImpl.CommonState commonState;

	public QueryStringPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new QueryStringPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public QueryStringPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new QueryStringPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
