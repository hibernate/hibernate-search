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


class RangePredicateContextImpl<N, CTX, C> implements RangePredicateContext<N>, SearchPredicateContributor<CTX, C> {

	private final RangePredicateFieldSetContextImpl.CommonState<N, CTX, C> commonState;

	RangePredicateContextImpl(SearchPredicateFactory<CTX, C> factory, Supplier<N> nextContextProvider) {
		this.commonState = new RangePredicateFieldSetContextImpl.CommonState<>( factory, nextContextProvider );
	}

	@Override
	public RangePredicateFieldSetContext<N> onFields(String ... absoluteFieldPaths) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public void contribute(CTX context, C collector) {
		commonState.contribute( context, collector );
	}
}
