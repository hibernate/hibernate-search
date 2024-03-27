/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchRequireStep;
import org.hibernate.search.engine.search.predicate.spi.MinimumShouldMatchBuilder;
import org.hibernate.search.util.common.impl.Contracts;

final class MinimumShouldMatchConditionStepImpl<N>
		implements MinimumShouldMatchConditionStep<N>,
		MinimumShouldMatchRequireStep<N>, MinimumShouldMatchMoreStep<N> {

	private final MinimumShouldMatchBuilder builder;
	private final N nextStep;
	private int ignoreConstraintCeiling = 0;

	MinimumShouldMatchConditionStepImpl(MinimumShouldMatchBuilder builder, N nextStep) {
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
