/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;
import org.hibernate.search.engine.search.predicate.spi.WithParametersPredicateBuilder;

public class WithParametersPredicateFinalStep extends AbstractPredicateFinalStep {

	private final WithParametersPredicateBuilder builder;

	public WithParametersPredicateFinalStep(SearchPredicateDslContext<?> dslContext,
			Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator) {
		super( dslContext );
		SearchPredicateIndexScope<?> scope = dslContext.scope();
		this.builder = scope.predicateBuilders().withParameters();
		builder.creator( predicateCreator );
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}

}
