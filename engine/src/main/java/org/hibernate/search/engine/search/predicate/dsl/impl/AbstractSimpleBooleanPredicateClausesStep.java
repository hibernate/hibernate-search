/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.GenericSimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;

public abstract class AbstractSimpleBooleanPredicateClausesStep<
		S extends C,
		C extends SimpleBooleanPredicateClausesCollector<?>>
		extends AbstractPredicateFinalStep
		implements GenericSimpleBooleanPredicateClausesStep<S, C> {

	public enum SimpleBooleanPredicateOperator
			implements BiConsumer<BooleanPredicateBuilder, SearchPredicate> {
		AND {
			@Override
			public void accept(BooleanPredicateBuilder builder,
					SearchPredicate searchPredicate) {
				builder.must( searchPredicate );
			}
		},
		OR {
			@Override
			public void accept(BooleanPredicateBuilder builder,
					SearchPredicate searchPredicate) {
				builder.should( searchPredicate );
			}
		}
	}

	private final SimpleBooleanPredicateOperator operator;

	private final BooleanPredicateBuilder builder;

	private final SearchPredicateFactory factory;

	AbstractSimpleBooleanPredicateClausesStep(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
		super( dslContext );
		this.operator = operator;
		this.builder = dslContext.scope().predicateBuilders().bool();
		this.factory = factory;
	}

	protected abstract S self();

	@Override
	public S add(SearchPredicate searchPredicate) {
		operator.accept( builder, searchPredicate );
		return self();
	}

	@Override
	public S add(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		return add( clauseContributor.apply( factory ) );
	}

	public S boost(float boost) {
		builder.boost( boost );
		return self();
	}

	public S constantScore() {
		builder.constantScore();
		return self();
	}

	@Override
	public S with(Consumer<? super C> contributor) {
		contributor.accept( self() );
		return self();
	}

	@Override
	public boolean hasClause() {
		return builder.hasClause();
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}
}
