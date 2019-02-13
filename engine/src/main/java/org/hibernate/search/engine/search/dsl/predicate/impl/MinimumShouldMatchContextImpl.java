/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchConditionContext;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchContext;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchNonEmptyContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;

final class MinimumShouldMatchContextImpl<N> implements MinimumShouldMatchContext<N>,
		MinimumShouldMatchConditionContext<N>, MinimumShouldMatchNonEmptyContext<N> {

	private final BooleanJunctionPredicateBuilder<?> builder;
	private final N nextContext;
	private int ignoreConstraintCeiling = 0;

	MinimumShouldMatchContextImpl(BooleanJunctionPredicateBuilder<?> builder, N nextContext) {
		this.builder = builder;
		this.nextContext = nextContext;
	}

	@Override
	public MinimumShouldMatchConditionContext<N> ifMoreThan(int ignoreConstraintCeiling) {
		Contracts.assertPositiveOrZero( ignoreConstraintCeiling, "ignoreConstraintCeiling" );
		this.ignoreConstraintCeiling = ignoreConstraintCeiling;
		return this;
	}

	@Override
	public MinimumShouldMatchNonEmptyContext<N> thenRequireNumber(int matchingClausesNumber) {
		builder.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
		return this;
	}

	@Override
	public MinimumShouldMatchNonEmptyContext<N> thenRequirePercent(int matchingClausesPercent) {
		builder.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
		return this;
	}

	@Override
	public N end() {
		return nextContext;
	}

}
