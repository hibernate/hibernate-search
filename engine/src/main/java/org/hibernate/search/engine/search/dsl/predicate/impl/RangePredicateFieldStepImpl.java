/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

class RangePredicateFieldStepImpl<B> implements RangePredicateFieldStep {

	private final RangePredicateFieldMoreStepImpl.CommonState<B> commonState;

	RangePredicateFieldStepImpl(SearchPredicateBuilderFactory<?, B> builderFactory) {
		this.commonState = new RangePredicateFieldMoreStepImpl.CommonState<>( builderFactory );
	}

	@Override
	public RangePredicateFieldMoreStep fields(String ... absoluteFieldPaths) {
		return new RangePredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
