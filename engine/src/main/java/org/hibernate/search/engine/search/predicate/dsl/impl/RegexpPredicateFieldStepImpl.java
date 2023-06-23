/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class RegexpPredicateFieldStepImpl
		implements RegexpPredicateFieldStep<RegexpPredicateFieldMoreStep<?, ?>> {

	private final RegexpPredicateFieldMoreStepImpl.CommonState commonState;

	public RegexpPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new RegexpPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public RegexpPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new RegexpPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
