/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.GenericBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;

abstract class AbstractBooleanPredicateClausesStep<S extends C, C extends BooleanPredicateOptionsCollector<?>>
		extends AbstractPredicateFinalStep
		implements GenericBooleanPredicateClausesStep<S, C> {

	private final SearchPredicateFactory factory;

	private final BooleanPredicateBuilder builder;

	private final MinimumShouldMatchConditionStepImpl<S> minimumShouldMatchStep;

	public AbstractBooleanPredicateClausesStep(SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
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
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		must( clauseContributor.apply( factory ) );
		return self();
	}

	@Override
	public S mustNot(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		mustNot( clauseContributor.apply( factory ) );
		return self();
	}

	@Override
	public S should(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		should( clauseContributor.apply( factory ) );
		return self();
	}

	@Override
	public S filter(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
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
