/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


class RangePredicateContextImpl<N, C> implements RangePredicateContext<N>, SearchPredicateContributor<C> {

	private final RangePredicateFieldSetContextImpl.CommonState<N, C> commonState;

	RangePredicateContextImpl(SearchPredicateFactory<C> factory, Supplier<N> nextContextProvider) {
		this.commonState = new RangePredicateFieldSetContextImpl.CommonState<>( factory, nextContextProvider );
	}

	@Override
	public RangePredicateFieldSetContext<N> onFields(String ... fieldName) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( fieldName ) );
	}

	@Override
	public void contribute(C collector) {
		commonState.contribute( collector );
	}
}
