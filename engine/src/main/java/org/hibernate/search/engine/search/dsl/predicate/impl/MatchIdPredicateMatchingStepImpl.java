/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.dsl.predicate.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchIdPredicateMatchingMoreStep;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class MatchIdPredicateMatchingStepImpl<B>
		extends AbstractPredicateFinalStep<B>
		implements MatchIdPredicateMatchingStep, MatchIdPredicateMatchingMoreStep {

	private final MatchIdPredicateBuilder<B> matchIdBuilder;

	MatchIdPredicateMatchingStepImpl(SearchPredicateBuilderFactory<?, B> builderFactory) {
		super( builderFactory );
		this.matchIdBuilder = builderFactory.id();
	}

	@Override
	public MatchIdPredicateMatchingMoreStep matching(Object value) {
		matchIdBuilder.value( value );
		return this;
	}

	@Override
	protected B toImplementation() {
		return matchIdBuilder.toImplementation();
	}
}
