/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class QueryStringPredicateFieldStepImpl
		implements QueryStringPredicateFieldStep<QueryStringPredicateFieldMoreStep<?, ?>> {

	private final QueryStringPredicateFieldMoreStepImpl.CommonState commonState;

	public QueryStringPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new QueryStringPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public QueryStringPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new QueryStringPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
