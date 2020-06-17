/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchIdPredicateMatchingMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchIdPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

class MatchIdPredicateMatchingStepImpl
		extends AbstractPredicateFinalStep
		implements MatchIdPredicateMatchingStep<MatchIdPredicateMatchingStepImpl>,
				MatchIdPredicateMatchingMoreStep<MatchIdPredicateMatchingStepImpl, MatchIdPredicateOptionsStep<?>> {

	private final MatchIdPredicateBuilder matchIdBuilder;

	MatchIdPredicateMatchingStepImpl(SearchPredicateBuilderFactory<?> builderFactory) {
		super( builderFactory );
		this.matchIdBuilder = builderFactory.id();
	}

	@Override
	public MatchIdPredicateMatchingStepImpl matching(Object value, ValueConvert convert) {
		matchIdBuilder.value( value, convert );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		return matchIdBuilder.build();
	}
}
