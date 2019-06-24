/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class BooleanPredicateClausesStepImpl<B>
		extends AbstractPredicateFinalStep<B>
		implements BooleanPredicateClausesStep {

	private final SearchPredicateFactoryContext factoryContext;

	private final BooleanJunctionPredicateBuilder<B> builder;

	private final MinimumShouldMatchConditionStepImpl<BooleanPredicateClausesStep> minimumShouldMatchStep;

	BooleanPredicateClausesStepImpl(SearchPredicateBuilderFactory<?, B> factory, SearchPredicateFactoryContext factoryContext) {
		super( factory );
		this.factoryContext = factoryContext;
		this.builder = factory.bool();
		this.minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( builder, this );
	}

	@Override
	public BooleanPredicateClausesStep boostedTo(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep withConstantScore() {
		builder.withConstantScore();
		return this;
	}

	@Override
	public BooleanPredicateClausesStep must(SearchPredicate searchPredicate) {
		builder.must( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep mustNot(SearchPredicate searchPredicate) {
		builder.mustNot( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep should(SearchPredicate searchPredicate) {
		builder.should( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep filter(SearchPredicate searchPredicate) {
		builder.filter( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep must(
			Function<? super SearchPredicateFactoryContext, ? extends PredicateFinalStep> clauseContributor) {
		must( clauseContributor.apply( factoryContext ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep mustNot(
			Function<? super SearchPredicateFactoryContext, ? extends PredicateFinalStep> clauseContributor) {
		mustNot( clauseContributor.apply( factoryContext ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep should(
			Function<? super SearchPredicateFactoryContext, ? extends PredicateFinalStep> clauseContributor) {
		should( clauseContributor.apply( factoryContext ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep filter(
			Function<? super SearchPredicateFactoryContext, ? extends PredicateFinalStep> clauseContributor) {
		filter( clauseContributor.apply( factoryContext ) );
		return this;
	}

	@Override
	public MinimumShouldMatchConditionStep<? extends BooleanPredicateClausesStep> minimumShouldMatch() {
		return minimumShouldMatchStep;
	}

	@Override
	public BooleanPredicateClausesStep minimumShouldMatch(
			Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor) {
		constraintContributor.accept( minimumShouldMatchStep );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}

}
