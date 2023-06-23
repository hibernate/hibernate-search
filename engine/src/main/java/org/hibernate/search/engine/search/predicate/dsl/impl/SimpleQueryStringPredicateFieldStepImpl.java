/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class SimpleQueryStringPredicateFieldStepImpl
		implements SimpleQueryStringPredicateFieldStep<SimpleQueryStringPredicateFieldMoreStep<?, ?>> {

	private final SimpleQueryStringPredicateFieldMoreStepImpl.CommonState commonState;

	public SimpleQueryStringPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new SimpleQueryStringPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new SimpleQueryStringPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
