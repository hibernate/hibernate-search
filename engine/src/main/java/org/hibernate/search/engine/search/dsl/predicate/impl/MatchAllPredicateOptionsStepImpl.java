/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class MatchAllPredicateOptionsStepImpl<B>
		extends AbstractPredicateFinalStep<B>
		implements MatchAllPredicateOptionsStep {

	private final SearchPredicateFactoryContext factoryContext;

	private final MatchAllPredicateBuilder<B> matchAllBuilder;
	private MatchAllExceptState exceptState;

	MatchAllPredicateOptionsStepImpl(SearchPredicateBuilderFactory<?, B> factory, SearchPredicateFactoryContext factoryContext) {
		super( factory );
		this.factoryContext = factoryContext;
		this.matchAllBuilder = factory.matchAll();
	}

	@Override
	public MatchAllPredicateOptionsStep boostedTo(float boost) {
		this.matchAllBuilder.boost( boost );
		return this;
	}

	@Override
	public MatchAllPredicateOptionsStep except(SearchPredicate searchPredicate) {
		getExceptState().addClause( searchPredicate );
		return this;
	}

	@Override
	public MatchAllPredicateOptionsStep except(
			Function<? super SearchPredicateFactoryContext, ? extends PredicateFinalStep> clauseContributor) {
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

		private final BooleanJunctionPredicateBuilder<B> booleanBuilder;
		private final List<B> clauseBuilders = new ArrayList<>();

		MatchAllExceptState() {
			this.booleanBuilder = MatchAllPredicateOptionsStepImpl.this.factory.bool();
		}

		void addClause(Function<? super SearchPredicateFactoryContext, ? extends PredicateFinalStep> clauseContributor) {
			addClause( clauseContributor.apply( factoryContext ).toPredicate() );
		}

		void addClause(SearchPredicate predicate) {
			clauseBuilders.add( factory.toImplementation( predicate ) );
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
