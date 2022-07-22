/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanOperatorPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanOperatorPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;

public final class SimpleBooleanOperatorPredicateClausesStepImpl
		extends AbstractPredicateFinalStep
		implements SimpleBooleanOperatorPredicateClausesStep<SimpleBooleanOperatorPredicateClausesStepImpl> {

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

	public SimpleBooleanOperatorPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
		super( dslContext );
		this.operator = operator;
		this.builder = dslContext.scope().predicateBuilders().bool();
		this.factory = factory;
	}

	public SimpleBooleanOperatorPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory,
			SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicates) {
		this( operator, dslContext, factory );
		add( firstSearchPredicate );
		for ( SearchPredicate step : otherSearchPredicates ) {
			add( step );
		}
	}

	public SimpleBooleanOperatorPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory,
			PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicates) {
		this( operator, dslContext, factory );
		add( firstSearchPredicate.toPredicate() );
		for ( PredicateFinalStep step : otherSearchPredicates ) {
			add( step.toPredicate() );
		}
	}

	@Override
	public SimpleBooleanOperatorPredicateClausesStepImpl add(SearchPredicate searchPredicate) {
		operator.accept( builder, searchPredicate );
		return this;
	}

	@Override
	public SimpleBooleanOperatorPredicateClausesStepImpl add(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		return add( clauseContributor.apply( factory ) );
	}

	public SimpleBooleanOperatorPredicateClausesStepImpl boost(float boost) {
		builder.boost( boost );
		return this;
	}

	public SimpleBooleanOperatorPredicateClausesStepImpl constantScore() {
		builder.constantScore();
		return this;
	}

	@Override
	public SimpleBooleanOperatorPredicateClausesStepImpl with(
			Consumer<? super SimpleBooleanOperatorPredicateClausesCollector<?>> contributor) {
		contributor.accept( this );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}
}
