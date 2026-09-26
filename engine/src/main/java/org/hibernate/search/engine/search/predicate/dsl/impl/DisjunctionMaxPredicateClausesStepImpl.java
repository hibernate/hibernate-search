/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.DisjunctionMaxPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.DisjunctionMaxPredicateBuilder;

public final class DisjunctionMaxPredicateClausesStepImpl<SR>
		extends AbstractPredicateFinalStep
		implements DisjunctionMaxPredicateClausesStep<SR, DisjunctionMaxPredicateClausesStepImpl<SR>> {

	private final DisjunctionMaxPredicateBuilder builder;
	private final TypedSearchPredicateFactory<SR> factory;

	public DisjunctionMaxPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory) {
		super( dslContext );
		this.builder = dslContext.scope().predicateBuilders().disjunctionMax();
		this.factory = factory;
	}

	public DisjunctionMaxPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory,
			SearchPredicate firstSearchPredicate, SearchPredicate... otherSearchPredicates) {
		this( dslContext, factory );
		add( firstSearchPredicate );
		for ( SearchPredicate other : otherSearchPredicates ) {
			add( other );
		}
	}

	public DisjunctionMaxPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory,
			PredicateFinalStep firstSearchPredicate, PredicateFinalStep... otherSearchPredicates) {
		this( dslContext, factory );
		add( firstSearchPredicate );
		for ( PredicateFinalStep other : otherSearchPredicates ) {
			add( other );
		}
	}

	@Override
	public DisjunctionMaxPredicateClausesStepImpl<SR> add(PredicateFinalStep searchPredicate) {
		return add( searchPredicate.toPredicate() );
	}

	@Override
	public DisjunctionMaxPredicateClausesStepImpl<SR> add(SearchPredicate searchPredicate) {
		builder.disjunct( searchPredicate );
		return this;
	}

	@Override
	public DisjunctionMaxPredicateClausesStepImpl<SR> add(
			Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor) {
		return add( clauseContributor.apply( factory ) );
	}

	@Override
	public DisjunctionMaxPredicateClausesStepImpl<SR> with(
			Consumer<? super DisjunctionMaxPredicateClausesStepImpl<SR>> contributor) {
		contributor.accept( this );
		return this;
	}

	@Override
	public boolean hasClause() {
		return builder.hasClause();
	}

	@Override
	public DisjunctionMaxPredicateClausesStepImpl<SR> tieBreaker(float tieBreaker) {
		builder.tieBreaker( tieBreaker );
		return this;
	}

	@Override
	public DisjunctionMaxPredicateClausesStepImpl<SR> boost(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public DisjunctionMaxPredicateClausesStepImpl<SR> constantScore() {
		builder.constantScore();
		return this;
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}
}
