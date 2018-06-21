/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


class SpatialWithinPredicateContextImpl<N, CTX, C> implements SpatialWithinPredicateContext<N>, SearchPredicateContributor<CTX, C> {

	private final SpatialWithinPredicateFieldSetContextImpl.CommonState<N, CTX, C> commonState;

	SpatialWithinPredicateContextImpl(SearchPredicateFactory<CTX, C> factory, Supplier<N> nextContextProvider) {
		this.commonState = new SpatialWithinPredicateFieldSetContextImpl.CommonState<>( factory, nextContextProvider );
	}

	@Override
	public SpatialWithinPredicateFieldSetContext<N> onFields(String ... absoluteFieldPaths) {
		return new SpatialWithinPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public void contribute(CTX context, C collector) {
		commonState.contribute( context, collector );
	}
}
