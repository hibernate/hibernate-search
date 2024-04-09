/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreGenericStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.reference.TypedFieldReference;

public final class MatchPredicateFieldStepImpl implements MatchPredicateFieldStep<MatchPredicateFieldMoreStep<?, ?>> {

	private final SearchPredicateDslContext<?> dslContext;

	public MatchPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public MatchPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return AbstractMatchPredicateFieldMoreStep.create( dslContext, fieldPaths );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> MatchPredicateFieldMoreGenericStep<?, ?, T, TypedFieldReference<T>> fields(TypedFieldReference<T>... fields) {
		return AbstractMatchPredicateFieldMoreStep.create( dslContext, fields );
	}
}
