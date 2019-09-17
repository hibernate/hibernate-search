/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class MatchAllPredicateOptionsStepImpl<B>
		extends AbstractPredicateFinalStep<B>
		implements MatchAllPredicateOptionsStep<MatchAllPredicateOptionsStep<?>> {

	private final SearchPredicateFactory factory;

	private final MatchAllPredicateBuilder<B> matchAllBuilder;
	private MatchAllExceptState exceptState;

	MatchAllPredicateOptionsStepImpl(SearchPredicateBuilderFactory<?, B> builderFactory,
			SearchPredicateFactory factory) {
		super( builderFactory );
		this.factory = factory;
		this.matchAllBuilder = builderFactory.matchAll();
	}

	@Override
	public MatchAllPredicateOptionsStep<?> boost(float boost) {
		this.matchAllBuilder.boost( boost );
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
	protected B toImplementation() {
		if ( exceptState != null ) {
			return exceptState.toImplementation( matchAllBuilder.toImplementation() );
		}
		else {
			return matchAllBuilder.toImplementation();
		}
	}

	private MatchAllExceptState getExceptState() {
		if ( exceptState == null ) {
			exceptState = new MatchAllExceptState();
		}
		return exceptState;
	}

	private class MatchAllExceptState {

		private final BooleanPredicateBuilder<B> booleanBuilder;
		private final List<B> clauseBuilders = new ArrayList<>();

		MatchAllExceptState() {
			this.booleanBuilder = MatchAllPredicateOptionsStepImpl.this.builderFactory.bool();
		}

		void addClause(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
			addClause( clauseContributor.apply( factory ).toPredicate() );
		}

		void addClause(SearchPredicate predicate) {
			clauseBuilders.add( builderFactory.toImplementation( predicate ) );
		}

		B toImplementation(B matchAllBuilder) {
			booleanBuilder.must( matchAllBuilder );
			for ( B builder : clauseBuilders ) {
				booleanBuilder.mustNot( builder );
			}
			return booleanBuilder.toImplementation();
		}

	}
}
