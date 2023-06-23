/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchRequireStep;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;

final class MinimumShouldMatchConditionStepImpl<N>
		implements MinimumShouldMatchConditionStep<N>,
		MinimumShouldMatchRequireStep<N>, MinimumShouldMatchMoreStep<N> {

	private final BooleanPredicateBuilder builder;
	private final N nextStep;
	private int ignoreConstraintCeiling = 0;

	MinimumShouldMatchConditionStepImpl(BooleanPredicateBuilder builder, N nextStep) {
		this.builder = builder;
		this.nextStep = nextStep;
	}

	@Override
	public MinimumShouldMatchRequireStep<N> ifMoreThan(int ignoreConstraintCeiling) {
		Contracts.assertPositiveOrZero( ignoreConstraintCeiling, "ignoreConstraintCeiling" );
		this.ignoreConstraintCeiling = ignoreConstraintCeiling;
		return this;
	}

	@Override
	public MinimumShouldMatchMoreStep<N> thenRequireNumber(int matchingClausesNumber) {
		builder.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
		return this;
	}

	@Override
	public MinimumShouldMatchMoreStep<N> thenRequirePercent(int matchingClausesPercent) {
		builder.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
		return this;
	}

	@Override
	public N end() {
		return nextStep;
	}

}
