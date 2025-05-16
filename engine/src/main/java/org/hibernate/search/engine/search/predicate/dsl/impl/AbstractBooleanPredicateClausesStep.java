/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.GenericBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;

abstract class AbstractBooleanPredicateClausesStep<SR, S extends C, C extends BooleanPredicateOptionsCollector<SR, ?>>
		extends AbstractPredicateFinalStep
		implements GenericBooleanPredicateClausesStep<SR, S, C> {

	private final TypedSearchPredicateFactory<SR> factory;

	private final BooleanPredicateBuilder builder;

	private final MinimumShouldMatchConditionStepImpl<S> minimumShouldMatchStep;

	public AbstractBooleanPredicateClausesStep(SearchPredicateDslContext<?> dslContext,
			TypedSearchPredicateFactory<SR> factory) {
		super( dslContext );
		this.factory = factory;
		this.builder = dslContext.scope().predicateBuilders().bool();
		this.minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( builder, self() );
	}

	protected abstract S self();

	@Override
	public S boost(float boost) {
		builder.boost( boost );
		return self();
	}

	@Override
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
	public S must(SearchPredicate searchPredicate) {
		builder.must( searchPredicate );
		return self();
	}

	@Override
	public S mustNot(SearchPredicate searchPredicate) {
		builder.mustNot( searchPredicate );
		return self();
	}

	@Override
	public S should(SearchPredicate searchPredicate) {
		builder.should( searchPredicate );
		return self();
	}

	@Override
	public S filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return self();
	}

	@Override
	public S must(
			Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor) {
		must( clauseContributor.apply( factory ) );
		return self();
	}

	@Override
	public S mustNot(
			Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor) {
		mustNot( clauseContributor.apply( factory ) );
		return self();
	}

	@Override
	public S should(
			Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor) {
		should( clauseContributor.apply( factory ) );
		return self();
	}

	@Override
	public S filter(
			Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor) {
		filter( clauseContributor.apply( factory ) );
		return self();
	}

	@Override
	public MinimumShouldMatchConditionStep<S> minimumShouldMatch() {
		return minimumShouldMatchStep;
	}

	@Override
	public S minimumShouldMatch(
			Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor) {
		constraintContributor.accept( minimumShouldMatchStep );
		return self();
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}

	@Override
	public boolean hasClause() {
		return builder.hasClause();
	}

}
