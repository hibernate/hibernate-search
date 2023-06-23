/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class TermsPredicateFieldStepImpl implements TermsPredicateFieldStep<TermsPredicateFieldMoreStep<?, ?>> {

	private final TermsPredicateFieldMoreStepImpl.CommonState commonState;

	public TermsPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new TermsPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public TermsPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new TermsPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
