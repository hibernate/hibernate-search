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
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;


public final class BooleanPredicateClausesStepImpl
		extends AbstractPredicateFinalStep
		implements BooleanPredicateClausesStep<BooleanPredicateClausesStep<?>> {

	private final SearchPredicateFactory factory;

	private final BooleanPredicateBuilder builder;

	private final MinimumShouldMatchConditionStepImpl<BooleanPredicateClausesStep<?>> minimumShouldMatchStep;

	public BooleanPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
		super( dslContext );
		this.factory = factory;
		this.builder = dslContext.scope().predicateBuilders().bool();
		this.minimumShouldMatchStep = new MinimumShouldMatchConditionStepImpl<>( builder, this );
	}

	@Override
	public BooleanPredicateClausesStep<?> boost(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> constantScore() {
		builder.constantScore();
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> with(Consumer<? super BooleanPredicateOptionsCollector<?>> contributor) {
		contributor.accept( this );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> must(SearchPredicate searchPredicate) {
		builder.must( searchPredicate );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> mustNot(SearchPredicate searchPredicate) {
		builder.mustNot( searchPredicate );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> should(SearchPredicate searchPredicate) {
		builder.should( searchPredicate );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> filter(SearchPredicate searchPredicate) {
		builder.filter( searchPredicate );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> must(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		must( clauseContributor.apply( factory ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> mustNot(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		mustNot( clauseContributor.apply( factory ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> should(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		should( clauseContributor.apply( factory ) );
		return this;
	}

	@Override
	public BooleanPredicateClausesStep<?> filter(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		filter( clauseContributor.apply( factory ) );
		return this;
	}

	@Override
	public MinimumShouldMatchConditionStep<BooleanPredicateClausesStep<?>> minimumShouldMatch() {
		return minimumShouldMatchStep;
	}

	@Override
	public BooleanPredicateClausesStep<?> minimumShouldMatch(
			Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor) {
		constraintContributor.accept( minimumShouldMatchStep );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}

}
