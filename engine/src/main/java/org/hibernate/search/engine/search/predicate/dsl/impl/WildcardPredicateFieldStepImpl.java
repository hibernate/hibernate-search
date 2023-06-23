/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class WildcardPredicateFieldStepImpl
		implements WildcardPredicateFieldStep<WildcardPredicateFieldMoreStep<?, ?>> {

	private final WildcardPredicateFieldMoreStepImpl.CommonState commonState;

	public WildcardPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new WildcardPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public WildcardPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new WildcardPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
