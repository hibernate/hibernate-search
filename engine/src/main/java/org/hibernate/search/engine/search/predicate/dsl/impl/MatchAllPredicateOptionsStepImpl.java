/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

public final class MatchAllPredicateOptionsStepImpl
		extends AbstractPredicateFinalStep
		implements MatchAllPredicateOptionsStep<MatchAllPredicateOptionsStep<?>> {

	private final SearchPredicateFactory factory;

	private final MatchAllPredicateBuilder matchAllBuilder;
	private MatchAllExceptState exceptState;
	private Float boost;
	private boolean constantScore = false;

	public MatchAllPredicateOptionsStepImpl(SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
		super( dslContext );
		this.factory = factory;
		this.matchAllBuilder = dslContext.scope().predicateBuilders().matchAll();
	}

	@Override
	public MatchAllPredicateOptionsStep<?> boost(float boost) {
		this.boost = boost;
		return this;
	}

	@Override
	public MatchAllPredicateOptionsStep<?> constantScore() {
		this.constantScore = true;
		return this;
	}

	@Override
	public MatchAllPredicateOptionsStep<?> except(SearchPredicate searchPredicate) {
		getExceptState().addClause( searchPredicate );
		return this;
	}

	@Override
	public MatchAllPredicateOptionsStep<?> except(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		getExceptState().addClause( clauseContributor );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		SearchPredicateBuilder builder;
		if ( exceptState != null ) {
			builder = exceptState.builder( matchAllBuilder.build() );
		}
		else {
			builder = matchAllBuilder;
		}
		if ( constantScore ) {
			builder.constantScore();
		}
		if ( boost != null ) {
			builder.boost( boost );
		}
		return builder.build();
	}

	private MatchAllExceptState getExceptState() {
		if ( exceptState == null ) {
			exceptState = new MatchAllExceptState();
		}
		return exceptState;
	}

	private class MatchAllExceptState {

		private final BooleanPredicateBuilder booleanBuilder;

		MatchAllExceptState() {
			this.booleanBuilder = dslContext.scope().predicateBuilders().bool();
		}

		void addClause(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
			addClause( clauseContributor.apply( factory ).toPredicate() );
		}

		void addClause(SearchPredicate predicate) {
			booleanBuilder.mustNot( predicate );
		}

		SearchPredicateBuilder builder(SearchPredicate matchAll) {
			booleanBuilder.must( matchAll );
			return booleanBuilder;
		}

	}
}
