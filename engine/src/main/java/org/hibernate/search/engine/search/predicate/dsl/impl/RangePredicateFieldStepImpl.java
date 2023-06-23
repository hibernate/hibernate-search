/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class RangePredicateFieldStepImpl implements RangePredicateFieldStep<RangePredicateFieldMoreStep<?, ?>> {

	private final RangePredicateFieldMoreStepImpl.CommonState commonState;

	public RangePredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new RangePredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public RangePredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new RangePredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
