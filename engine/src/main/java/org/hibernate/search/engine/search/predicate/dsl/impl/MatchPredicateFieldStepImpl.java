/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class MatchPredicateFieldStepImpl implements MatchPredicateFieldStep<MatchPredicateFieldMoreStep<?, ?>> {

	private final MatchPredicateFieldMoreStepImpl.CommonState commonState;

	public MatchPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new MatchPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public MatchPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new MatchPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
