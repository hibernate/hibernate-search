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
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanOperatorPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanOperatorPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class SimpleBooleanOperatorPredicateClausesStepImpl
		extends AbstractSimpleBooleanOperatorPredicateClausesStep<SimpleBooleanOperatorPredicateClausesStepImpl, SimpleBooleanOperatorPredicateClausesCollector<?>>
		implements SimpleBooleanOperatorPredicateClausesStep<SimpleBooleanOperatorPredicateClausesStepImpl> {

	public SimpleBooleanOperatorPredicateClausesStepImpl(SimpleBooleanPredicateOperator operator,
			SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory factory) {
		super( operator, dslContext, factory );
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
	protected SimpleBooleanOperatorPredicateClausesStepImpl self() {
		return this;
	}
}
