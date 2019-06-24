/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class MatchPredicateFieldStepImpl<B> implements MatchPredicateFieldStep {

	private final MatchPredicateFieldMoreStepImpl.CommonState<B> commonState;

	MatchPredicateFieldStepImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.commonState = new MatchPredicateFieldMoreStepImpl.CommonState<>( factory );
	}

	@Override
	public MatchPredicateFieldMoreStep onFields(String ... absoluteFieldPaths) {
		return new MatchPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
