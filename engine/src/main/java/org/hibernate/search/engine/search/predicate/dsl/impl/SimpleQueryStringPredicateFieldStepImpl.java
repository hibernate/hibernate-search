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
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class SimpleQueryStringPredicateFieldStepImpl<B>
		implements SimpleQueryStringPredicateFieldStep<SimpleQueryStringPredicateFieldMoreStep<?, ?>> {

	private final SimpleQueryStringPredicateFieldMoreStepImpl.CommonState<B> commonState;

	SimpleQueryStringPredicateFieldStepImpl(SearchPredicateBuilderFactory<?, B> builderFactory) {
		this.commonState = new SimpleQueryStringPredicateFieldMoreStepImpl.CommonState<>( builderFactory );
	}

	@Override
	public SimpleQueryStringPredicateFieldMoreStep<?, ?> fields(String ... absoluteFieldPaths) {
		return new SimpleQueryStringPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
