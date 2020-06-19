/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;


class PhrasePredicateFieldStepImpl implements PhrasePredicateFieldStep<PhrasePredicateFieldMoreStep<?, ?>> {

	private final PhrasePredicateFieldMoreStepImpl.CommonState commonState;

	PhrasePredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new PhrasePredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public PhrasePredicateFieldMoreStep<?, ?> fields(String ... absoluteFieldPaths) {
		return new PhrasePredicateFieldMoreStepImpl( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
