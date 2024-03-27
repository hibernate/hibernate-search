/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.CommonQueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractStringQueryPredicateCommonState<
		T extends AbstractStringQueryPredicateCommonState<?, ?, ?>,
		S extends CommonQueryStringPredicateOptionsStep<T>,
		B extends CommonQueryStringPredicateBuilder>
		extends AbstractPredicateFinalStep
		implements CommonQueryStringPredicateOptionsStep<T> {
	protected final B builder;
	private final MinimumShouldMatchConditionStepImpl<T> minimumShouldMatchStep;

	AbstractStringQueryPredicateCommonState(SearchPredicateDslContext<?> dslContext) {
		super( dslContext );
		this.builder = createBuilder( dslContext );
		this.minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( builder, thisAsT() );
	}

	protected abstract B createBuilder(SearchPredicateDslContext<?> dslContext);

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}

	CommonQueryStringPredicateBuilder.FieldState field(String fieldPath) {
		return builder.field( fieldPath );
	}


	protected T matching(String queryString) {
		Contracts.assertNotNull( queryString, "queryString" );
		builder.queryString( queryString );
		return thisAsT();
	}

	@Override
	public T constantScore() {
		builder.constantScore();
		return thisAsT();
	}

	@Override
	public T boost(float boost) {
		builder.boost( boost );
		return thisAsT();
	}

	@Override
	public T defaultOperator(BooleanOperator operator) {
		builder.defaultOperator( operator );
		return thisAsT();
	}

	@Override
	public T analyzer(String analyzerName) {
		builder.analyzer( analyzerName );
		return thisAsT();
	}

	@Override
	public T skipAnalysis() {
		builder.skipAnalysis();
		return thisAsT();
	}

	@Override
	public MinimumShouldMatchConditionStep<? extends T> minimumShouldMatch() {
		return minimumShouldMatchStep;
	}

	@Override
	public T minimumShouldMatch(Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor) {
		constraintContributor.accept( minimumShouldMatchStep );
		return thisAsT();
	}

	protected abstract T thisAsT();
}
