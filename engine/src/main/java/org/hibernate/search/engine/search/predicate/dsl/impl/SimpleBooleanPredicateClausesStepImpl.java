/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class SimpleBooleanPredicateClausesStepImpl
		extends
		AbstractSimpleBooleanPredicateClausesStep<SimpleBooleanPredicateClausesStepImpl,
				SimpleBooleanPredicateClausesCollector<?>>
		implements SimpleBooleanPredicateClausesStep<SimpleBooleanPredicateClausesStepImpl> {

	public SimpleBooleanPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
		super( operator, dslContext, factory );
	}

	public SimpleBooleanPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
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

	public SimpleBooleanPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
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
	protected SimpleBooleanPredicateClausesStepImpl self() {
		return this;
	}
}
